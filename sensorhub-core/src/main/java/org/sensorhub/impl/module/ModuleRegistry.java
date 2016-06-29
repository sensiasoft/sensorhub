/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.module;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.IEventProducer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.config.IGlobalConfig;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.module.IModuleManager;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.module.ModuleEvent.Type;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.common.EventBus;
import org.sensorhub.utils.FileUtils;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * This class is in charge of loading all configured modules on startup
 * as well as dynamically loading/unloading modules on demand.
 * It also keeps lists of all loaded and available modules.
 * </p>
 * 
 * TODO implement global event manager for all modules ? 
 * TODO return weak references to modules ?
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 2, 2013
 */
public class ModuleRegistry implements IModuleManager<IModule<?>>, IEventProducer, IEventListener
{
    private static final Logger log = LoggerFactory.getLogger(ModuleRegistry.class);
    private static final String REGISTRY_SHUTDOWN_MSG = "Registry was shut down";
    public static final String ID = "MODULE_REGISTRY";
    public static final long SHUTDOWN_TIMEOUT_MS = 10000L;
    
    IModuleConfigRepository configRepos;
    Map<String, IModule<?>> loadedModules;
    IEventHandler eventHandler;
    ExecutorService asyncExec;
    volatile boolean shutdownCalled;
    
    
    public ModuleRegistry(IModuleConfigRepository configRepos, EventBus eventBus)
    {
        this.configRepos = configRepos;
        this.loadedModules = Collections.synchronizedMap(new LinkedHashMap<String, IModule<?>>());
        this.eventHandler = eventBus.registerProducer(ID);
        this.asyncExec = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                                10L, TimeUnit.SECONDS,
                                                new SynchronousQueue<Runnable>());
    }
    
    
    /**
     * Loads all enabled modules from configuration entries provided
     * by the specified IModuleConfigRepository
     */
    public synchronized void loadAllModules()
    {
        List<ModuleConfig> moduleConfs = configRepos.getAllModulesConfigurations();
        for (ModuleConfig config: moduleConfs)
        {
            try
            {
                loadModuleAsync(config, null);
            }
            catch (Exception e)
            {
                // continue loading other modules
            }
        }
    }
    
    
    /**
     * Instantiates and loads a module using the given configuration<br/>
     * This method is synchronous so it will block forever until the module is actually
     * loaded, and it will also wait for it to be started if 'autostart' was requested.
     * @param config Configuration class to use to instantiate the module
     * @return loaded module instance
     * @throws SensorHubException 
     */
    public IModule<?> loadModule(ModuleConfig config) throws SensorHubException
    {        
        return loadModule(config, Long.MAX_VALUE);
    }
    
    
    /**
     * Instantiates and loads a module using the given configuration<br/>
     * This method is synchronous so it will block until the module is actually loaded,
     * (and started if 'autostart' was true), the timeout occurs or an exception is thrown
     * @param config Configuration class to use to instantiate the module
     * @param timeOut Maximum time to wait for load and startup to complete (or <= 0 to wait forever)
     * @return loaded module instance
     * @throws SensorHubException 
     */
    public IModule<?> loadModule(ModuleConfig config, long timeOut) throws SensorHubException
    {        
        IModule<?> module = loadModuleAsync(config, null);
        
        if (config.autoStart)
        {
            if (!module.waitForState(ModuleState.STARTED, timeOut))
                throw new SensorHubException("Could not start module " + MsgUtils.moduleString(module) + " in the requested time frame");
        }
        
        return module;
    }
    
    
    /**
     * Instantiates and loads a module using the given configuration<br/>
     * This method is asynchronous so, when it returns without error, the module is guaranteed
     * to be loaded but not necessarilly initialized or started. The listener will be notified
     * when the module's state changes further.
     * @param config Configuration class to use to instantiate the module
     * @param listener Listener to register for receiving the module's events
     * @return loaded module instance (may not yet be started when this method returns)
     * @throws SensorHubException if no module with given ID can be found
     */
    @SuppressWarnings("rawtypes")
    public IModule<?> loadModuleAsync(ModuleConfig config, IEventListener listener) throws SensorHubException
    {
        if (config.id != null && loadedModules.containsKey(config.id))
            return loadedModules.get(config.id);
        
        IModule module = null;
        try
        {
            // generate a new ID if non was provided
            if (config.id == null)
                config.id = UUID.randomUUID().toString();
                        
            // instantiate module class
            module = (IModule)loadClass(config.moduleClass);
            log.debug("Module " + MsgUtils.moduleString(config) + " loaded");
            
            // set config
            module.setConfiguration(config);
            
            // listen to module lifecycle events
            module.registerListener(this);
            if (listener != null)
                module.registerListener(listener);
            
            // keep track of what modules are loaded
            loadedModules.put(config.id, module);            
        }
        catch (Exception e)
        {
            String msg = "Error while loading module " + config.name + "' [" + config.id + "]";
            log.error(msg, e);
            throw new SensorHubException(msg, e);
        }
        
        // launch init if autostart is set
        if (config.autoStart)
            initModuleAsync(config.id, null);
        
        return module;
    }
    
    
    /**
     * Loads any class by reflection
     * @param className
     * @return new object instantiated
     * @throws SensorHubException
     */
    public Object loadClass(String className) throws SensorHubException
    {
        try
        {
            Class<?> clazz = (Class<?>)Class.forName(className);
            return clazz.newInstance();
        }
        catch (ClassNotFoundException | IllegalAccessException | InstantiationException e)
        {
            throw new SensorHubException("Cannot instantiate class", e);
        }
    }
    
    
    /**
     * Creates a new module config class using information from a module provider
     * @param provider
     * @return the new configuration class
     * @throws SensorHubException
     */
    public ModuleConfig createModuleConfig(IModuleProvider provider) throws SensorHubException
    {
        Class<?> configClass = provider.getModuleConfigClass();
        
        try
        {
            ModuleConfig config = (ModuleConfig)configClass.newInstance();
            config.id = UUID.randomUUID().toString();
            config.moduleClass = provider.getModuleClass().getCanonicalName();
            config.name = "New " + provider.getModuleName();
            config.autoStart = false;
            return config;
        }
        catch (Exception e)
        {
            String msg = "Cannot create configuration class for module " + provider.getModuleName();
            log.error(msg, e);
            throw new SensorHubException(msg, e);
        }
    }
    
    
    @Override
    public boolean isModuleLoaded(String moduleID)
    {
        return loadedModules.containsKey(moduleID);
    }
    
    
    /**
     * Unloads a module instance.<br/>
     * This causes the module to be removed from registry but its last saved configuration
     * is kept as-is. Call {@link #saveConfiguration(ModuleConfig...)} first if you want to
     * keep the current config. 
     * @param moduleID
     * @throws SensorHubException
     */
    public void unloadModule(String moduleID) throws SensorHubException
    {
        stopModule(moduleID);        
        IModule<?> module = loadedModules.remove(moduleID);
        eventHandler.publishEvent(new ModuleEvent(module, Type.UNLOADED));
        log.debug("Module " + MsgUtils.moduleString(module) +  " unloaded");
    }
    
    
    /**
     * Initializes the module with the given local ID<br/>
     * This method is synchronous so it will block forever until the module is actually
     * initialized or an exception is thrown
     * @param moduleID Local ID of module to initialize
     * @return module instance corresponding to moduleID
     * @throws SensorHubException if an error occurs during init
     */
    public IModule<?> initModule(String moduleID) throws SensorHubException
    {
        return initModule(moduleID, Long.MAX_VALUE);
    }
    
    
    /**
     * Initializes the module with the given local ID<br/>
     * This method is synchronous so it will block until the module is actually initialized,
     * the timeout occurs or an exception is thrown
     * @param moduleID Local ID of module to initialize
     * @param timeOut Maximum time to wait for init to complete (or <= 0 to wait forever)
     * @return module Loaded module with the given moduleID
     * @throws SensorHubException if an error occurs during init
     */
    public IModule<?> initModule(String moduleID, long timeOut) throws SensorHubException
    {
        IModule<?> module = initModuleAsync(moduleID, null);
        if (!module.waitForState(ModuleState.INITIALIZED, timeOut))
            throw new SensorHubException("Could not initialize module " + MsgUtils.moduleString(module) + " in the requested time frame");
        return module;
    }
    
    
    /**
     * Initializes the module with the given local ID<br/>
     * This method is asynchronous so it returns immediately and the listener will be notified
     * when the module is actually initialized
     * @param moduleID Local ID of module to initialize
     * @param listener Listener to register for receiving the module's events
     * @return the module instance (may not yet be initialized when this method returns)
     * @throws SensorHubException if no module with given ID can be found
     */
    public IModule<?> initModuleAsync(final String moduleID, IEventListener listener) throws SensorHubException
    {
        @SuppressWarnings("rawtypes")
        final IModule module = getModuleById(moduleID);
        if (listener != null)
            module.registerListener(listener);
        
        try
        {
            // init module in separate thread
            asyncExec.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        module.requestInit();
                    }
                    catch (Exception e)
                    {
                        String msg = "Cannot initialize module " + MsgUtils.moduleString(module);
                        log.error(msg);
                    }
                }            
            });
        }
        catch (RejectedExecutionException e)
        {
            throw new SensorHubException(REGISTRY_SHUTDOWN_MSG, e);
        }
        
        return module;
    }
        
    
    /**
     * Starts the module with the given local ID<br/>
     * This method is synchronous so it will block forever until the module is actually
     * started or an exception is thrown
     * @param moduleID Local ID of module to start
     * @return module instance corresponding to moduleID
     * @throws SensorHubException if an error occurs during startup
     */
    public IModule<?> startModule(String moduleID) throws SensorHubException
    {
        return startModule(moduleID, Long.MAX_VALUE);
    }
    
    
    /**
     * Starts the module with the given local ID<br/>
     * This method is synchronous so it will block until the module is actually started,
     * the timeout occurs or an exception is thrown
     * @param moduleID Local ID of module to start
     * @param timeOut Maximum time to wait for startup to complete (or <= 0 to wait forever)
     * @return module Loaded module with the given moduleID
     * @throws SensorHubException if an error occurs during startup
     */
    public IModule<?> startModule(String moduleID, long timeOut) throws SensorHubException
    {
        IModule<?> module = startModuleAsync(moduleID, null);
        if (!module.waitForState(ModuleState.STARTED, timeOut))
            throw new SensorHubException("Could not start module " + MsgUtils.moduleString(module) + " in the requested time frame");
        return module;
    }
    
    
    /**
     * Starts the module with the given local ID<br/>
     * This method is asynchronous so it returns immediately and the listener will be notified
     * when the module is actually started
     * @param moduleID Local ID of module to start
     * @param listener Listener to register for receiving the module's events
     * @return the module instance (may not yet be started when this method returns)
     * @throws SensorHubException if no module with given ID can be found
     */
    public IModule<?> startModuleAsync(final String moduleID, IEventListener listener) throws SensorHubException
    {
        final IModule<?> module = getModuleById(moduleID);
        if (listener != null)
            module.registerListener(listener);
        
        startModuleAsync(module);
        return module;
    }
    
    
    protected void startModuleAsync(final IModule<?> module) throws SensorHubException
    {        
        try
        {
            // start module in separate thread
            asyncExec.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        if (!module.isInitialized())
                            module.requestInit();
                        module.requestStart();
                    }
                    catch (Exception e)
                    {
                        String msg = "Cannot start module " + MsgUtils.moduleString(module);
                        log.error(msg);
                    }
                }            
            });
        }
        catch (RejectedExecutionException e)
        {
            throw new SensorHubException(REGISTRY_SHUTDOWN_MSG, e);
        }
    }
    
    
    /**
     * Stops the module with the given local ID<br/>
     * This method is synchronous so it will block forever until the module is actually
     * stopped or an exception is thrown
     * @param moduleID Local ID of module to disable
     * @return module instance corresponding to moduleID
     * @throws SensorHubException if an error occurs during shutdown
     */
    public IModule<?> stopModule(String moduleID) throws SensorHubException
    {
        return stopModule(moduleID, Long.MAX_VALUE);
    }
    
    
    /**
     * Stops the module with the given local ID<br/>
     * This method is synchronous so it will block until the module is actually stopped,
     * the timeout occurs or an exception is thrown
     * @param moduleID Local ID of module to enable
     * @param timeOut Maximum time to wait for shutdown to complete (or <= 0 to wait forever)
     * @return module Loaded module with the given moduleID
     * @throws SensorHubException if an error occurs during shutdown
     */
    public IModule<?> stopModule(String moduleID, long timeOut) throws SensorHubException
    {
        IModule<?> module = stopModuleAsync(moduleID, null);
        if (!module.waitForState(ModuleState.STOPPED, timeOut))
            throw new SensorHubException("Could not stop module " + MsgUtils.moduleString(module) + " in the requested time frame");
        return module;
    }
    
    
    /**
     * Stops the module with the given local ID<br/>
     * This method is asynchronous so it returns immediately and the listener will be notified
     * when the module is actually stopped
     * @param moduleID Local ID of module to stop
     * @param listener Listener to register for receiving the module's events
     * @return the module instance (may not yet be stopped when this method returns)
     * @throws SensorHubException if no module with given ID can be found
     */
    public IModule<?> stopModuleAsync(final String moduleID, IEventListener listener) throws SensorHubException
    {
        final IModule<?> module = getModuleById(moduleID);
        if (listener != null)
            module.registerListener(listener);
        
        stopModuleAsync(module);
        return module;
    }
    
    
    protected void stopModuleAsync(final IModule<?> module) throws SensorHubException
    {        
        try
        {
            // stop module in separate thread
            asyncExec.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        module.requestStop();
                    }
                    catch (Exception e)
                    {
                        String msg = "Cannot stop module " + MsgUtils.moduleString(module);
                        log.error(msg);
                    }
                }            
            });
        }
        catch (Exception e)
        {
            throw new SensorHubException(REGISTRY_SHUTDOWN_MSG, e);
        }
    }
    
    
    public void updateModuleConfigAsync(final ModuleConfig config) throws SensorHubException
    {
        @SuppressWarnings("rawtypes")
        final IModule module = getModuleById(config.id);
        
        try
        {
            // stop module in separate thread
            asyncExec.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        module.updateConfig(config);
                    }
                    catch (Exception e)
                    {
                        String msg = "Cannot update configuration of module " + MsgUtils.moduleString(module);
                        log.error(msg);
                    }
                }            
            });
        }
        catch (Exception e)
        {
            throw new SensorHubException("Registry was shut down", e);
        }
    }

    
    
    /**
     * Removes the module with the given id
     * @param moduleID Local ID of module to delete
     * @throws SensorHubException 
     */
    public void destroyModule(String moduleID) throws SensorHubException
    {
        // we check both in live table and in config repository
        if (!loadedModules.containsKey(moduleID) && !configRepos.contains(moduleID))
            throw new SensorHubException("Unknown module " + moduleID);
        
        try
        {
            // stop it and call cleanup if it was loaded
            IModule<?> module = loadedModules.remove(moduleID);
            if (module != null)
            {
                module.stop();
                module.cleanup();
                getStateManager(moduleID).cleanup();
            }
            
            // remove conf from repo if it was saved 
            if (configRepos.contains(moduleID))
                configRepos.remove(moduleID);
            
            eventHandler.publishEvent(new ModuleEvent(module, Type.DELETED));
            log.debug("Module " + MsgUtils.moduleString(module) +  " removed");
        }
        catch (Exception e)
        {
            String msg = "Cannot destroy module " + moduleID;
            log.error(msg);
        }
    }
    
    
    /**
     * Save all modules current configuration to the repository
     */
    public void saveModulesConfiguration()
    {
        try
        {
            int numModules = loadedModules.size();
            ModuleConfig[] configList = new ModuleConfig[numModules];
            
            int i = 0;
            for (IModule<?> module: loadedModules.values())
            {
                configList[i] = module.getConfiguration();
                i++;
            }
            
            configRepos.update(configList);
        }
        catch (Exception e)
        {
            log.error("Error while saving SensorHub configuration", e);
            throw e;
        }
    }
    
    
    /**
     * Saves the given module configurations in the repository
     * @param configList 
     */
    public synchronized void saveConfiguration(ModuleConfig... configList)
    {
        for (ModuleConfig config: configList)
            configRepos.update(config);
    }
    
    
    @Override
    public synchronized Collection<IModule<?>> getLoadedModules()
    {
        return Collections.unmodifiableCollection(loadedModules.values());
    }
    
    
    @Override
    public IModule<?> getModuleById(String moduleID) throws SensorHubException
    {
        // load module if necessary
        if (!loadedModules.containsKey(moduleID))
        {
            if (configRepos.contains(moduleID))
                loadModuleAsync(configRepos.get(moduleID), null);
            else
                throw new SensorHubException("Unknown module " + moduleID);
        }
        
        return loadedModules.get(moduleID);
    }
    
    
    public WeakReference<? extends IModule<?>> getModuleRef(String moduleID) throws SensorHubException
    {
        IModule<?> module = getModuleById(moduleID);
        return new WeakReference<IModule<?>>(module);
    }
    
    
    @Override
    public synchronized Collection<ModuleConfig> getAvailableModules()
    {
        return Collections.unmodifiableCollection(configRepos.getAllModulesConfigurations());
    }
    
    
    /**
     * Retrieves list of all installed module types
     * @return list of module providers (not the module themselves)
     */
    public Collection<IModuleProvider> getInstalledModuleTypes()
    {
        ArrayList<IModuleProvider> installedModules = new ArrayList<IModuleProvider>();
        
        ServiceLoader<IModuleProvider> sl = ServiceLoader.load(IModuleProvider.class);
        try
        {
            for (IModuleProvider provider: sl)
                installedModules.add(provider);
        }
        catch (Throwable e)
        {
            log.error("Invalid reference to module descriptor", e);
        }
        
        return installedModules;
    }
    
    
    /**
     * Retrieves list of all installed module types that are sub-types
     * of the specified class
     * @param moduleClass Parent class of modules to search for
     * @return list of module providers (not the module themselves)
     */
    public Collection<IModuleProvider> getInstalledModuleTypes(Class<?> moduleClass)
    {
        ArrayList<IModuleProvider> installedModules = new ArrayList<IModuleProvider>();

        ServiceLoader<IModuleProvider> sl = ServiceLoader.load(IModuleProvider.class);
        for (IModuleProvider provider: sl)
        {
            if (moduleClass.isAssignableFrom(provider.getModuleClass()))
                installedModules.add(provider);
        }
        
        return installedModules;
    }
    
    
    /**
     * Shuts down all modules and the config repository
     * @param saveConfig If true, save current modules config
     * @param saveState If true, save current module state
     * @throws SensorHubException 
     */
    public synchronized void shutdown(boolean saveConfig, boolean saveState) throws SensorHubException
    {
        shutdownCalled = true;
        
        // do nothing if no modules have been loaded
        if (loadedModules.isEmpty())
            return;        
        
        long timeOutTime = System.currentTimeMillis() + SHUTDOWN_TIMEOUT_MS;
        log.info("Module registry shutdown initiated");
        log.info("Stopping all modules (saving config = {}, saving state = {})", saveConfig, saveState);
        
        // request stop for all modules
        for (IModule<?> module: getLoadedModules())
        {
            try
            {
                // save config if requested
                if (saveConfig)
                    configRepos.update(module.getConfiguration());
                
                // save state if requested
                if (saveState)
                {
                    try
                    {                   
                        IModuleStateManager stateManager = getStateManager(module.getLocalID());
                        if (stateManager != null)
                            module.saveState(stateManager);
                    }
                    catch (Exception ex)
                    {
                        log.error("State could not be saved for module " + MsgUtils.moduleString(module), ex);
                    }
                }
                
                // request to stop module
                stopModuleAsync(module);
            }
            catch (Exception e)
            {
                log.error("Error during shutdown", e);
            }
        }
        
        // shutdown executor once all tasks have been run
        asyncExec.shutdown();
        
        // wait for all modules to be stopped
        try
        {
            boolean allStopped = false;
            while (!allStopped)
            {
                allStopped = true;                
                for (IModule<?> module: getLoadedModules())
                {
                    ModuleState state = module.getCurrentState();
                    if (state != ModuleState.STOPPED)
                    {
                        allStopped = false;
                        break;
                    }
                }
                
                // stop if time out reached
                if (System.currentTimeMillis() > timeOutTime)
                    break;
                
                Thread.sleep(100);
            }
        }
        catch (InterruptedException e1)
        {
        }
        
        // unregister from all modules and warn if some could not stop
        boolean firstWarning = true;
        for (IModule<?> module: getLoadedModules())
        {
            module.unregisterListener(this);
            
            ModuleState state = module.getCurrentState();
            if (state != ModuleState.STOPPED)
            {
                if (firstWarning)
                {
                    log.warn("The following modules could not be stopped");
                    firstWarning = false;
                }
                
                log.warn(MsgUtils.moduleString(module));
            }
        } 
        
        // clear loaded modules
        loadedModules.clear();
        
        // make sure to clear all listeners in case they failed to unregister themselves
        eventHandler.clearAllListeners();
        
        // properly close config database
        configRepos.close();
    }
    
    
    /**
     * Returns the default state manager for the given module
     * @param moduleID
     * @return the state manager or null if no module data folder is specified in config
     */
    public IModuleStateManager getStateManager(String moduleID)
    {
        String moduleDataPath = SensorHub.getInstance().getConfig().getModuleDataPath();
        if (moduleDataPath != null)
            return new DefaultModuleStateManager(moduleDataPath, moduleID);
        else
            return null;
    }
    
    
    /**
     * Retrieves the folder where the module data should be stored 
     * @param moduleID Local ID of module
     * @return File object representing the folder or null if none was specified
     */
    public File getModuleDataFolder(String moduleID)
    {
        IGlobalConfig oshConfig = SensorHub.getInstance().getConfig();
        if (oshConfig == null)
            return null;
        
        String moduleDataRoot = SensorHub.getInstance().getConfig().getModuleDataPath();
        if (moduleDataRoot == null)
            return null;
        
        return new File(moduleDataRoot, FileUtils.safeFileName(moduleID));
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);        
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);        
    }


    @Override
    public void handleEvent(Event<?> e)
    {        
        if (e instanceof ModuleEvent)
        {
            IModule<?> module = ((ModuleEvent) e).getModule();
            String moduleString = MsgUtils.moduleString(module);
            
            switch (((ModuleEvent)e).getType())
            {
                case STATE_CHANGED:
                    switch (((ModuleEvent) e).getNewState())
                    {
                        case INITIALIZING:
                            log.debug("Initializing module " + moduleString);
                            break;
                            
                        case INITIALIZED:
                            log.info("Module " + moduleString + " initialized");
                            postInit(module);
                            break;
                            
                        case STARTING:
                            log.debug("Starting module " + moduleString);
                            break;
                            
                        case STARTED:
                            log.info("Module " + moduleString + " started");
                            break;
                            
                        case STOPPING:
                            log.debug("Stopping module " + moduleString);
                            break;
                            
                        case STOPPED:
                            log.info("Module " + moduleString + " stopped");
                            break;
                            
                        default:
                            break;
                    }
                    break;
                    
                case ERROR:
                    //log.error("Error in module " + moduleString, e);
                    break;
                    
                default:
                    break;
            }
            
            // forward all lifecycle events from modules loaded by this registry
            eventHandler.publishEvent(e);
        }
    }
    
    
    protected void postInit(IModule<?> module)
    {
        String moduleString = MsgUtils.moduleString(module);
        
        // load module state
        try
        {
            IModuleStateManager stateManager = getStateManager(module.getLocalID());
            if (stateManager != null)
                module.loadState(stateManager);
        }
        catch (SensorHubException e)
        {
            log.error("Cannot load state of module " + moduleString, e);
        }
        
        // also initiate startup if auto start was true
        try
        {
            if (!shutdownCalled && module.getConfiguration().autoStart)
                startModuleAsync(module);
        }
        catch (SensorHubException e)
        {
            log.error("Cannot auto-start module " + moduleString, e);
        }
        
    }
}
