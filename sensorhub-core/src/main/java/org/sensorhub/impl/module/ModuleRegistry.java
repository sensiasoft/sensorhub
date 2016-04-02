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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.IEventProducer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.module.IModuleManager;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.common.BasicEventHandler;
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
    
    IModuleConfigRepository configRepos;
    Map<String, IModule<?>> loadedModules;
    Map<String, Future<IModule<?>>> loadingModules;
    IEventHandler eventHandler;
    ExecutorService asyncExec;
    
    
    public ModuleRegistry(IModuleConfigRepository configRepos)
    {
        this.configRepos = configRepos;
        this.loadedModules = new LinkedHashMap<String, IModule<?>>();
        this.loadingModules = new HashMap<String, Future<IModule<?>>>();
        this.eventHandler = new BasicEventHandler();
        this.asyncExec = Executors.newFixedThreadPool(4);
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
                loadModule(config);
                //loadModuleAsync(config);
            }
            catch (Exception e)
            {
                log.error(e.getLocalizedMessage(), e);
            }
        }
    }
    
    
    /**
     * Instantiates one module using the given configuration
     * @param config Configuration class to use to instantiate the module
     * @return loaded module instance
     * @throws SensorHubException 
     */
    @SuppressWarnings("rawtypes")
    public IModule<?> loadModule(ModuleConfig config) throws SensorHubException
    {        
        if (config.id != null && loadedModules.containsKey(config.id))
            return loadedModules.get(config.id);
        
        try
        {
            // first load needed modules if necessary
            // not necessary since they can be loaded lazyly by getModuleById method
            //for (String moduleID: getReferencedModules(config))
            //    loadModule(configRepos.get(moduleID));
            
            // generate a new ID if non was provided
            if (config.id == null)
                config.id = UUID.randomUUID().toString();
                        
            // instantiate module class
            IModule module = (IModule)loadClass(config.moduleClass);
            
            // set LOADED state
            eventHandler.publishEvent(new ModuleEvent(module, ModuleState.LOADED));
            log.debug("Module '" + config.name + "' [" + config.id + "] loaded");
            
            // keep track of what modules are loaded
            loadedModules.put(config.id, module);
            
            // call init routine
            module.init(config);
                        
            // load saved module state
            module.loadState(getStateManager(config.id));
                        
            // set INITIALIZED state
            setModuleState(module, ModuleState.INITIALIZED);
            log.debug("Module " + MsgUtils.moduleString(module) +  " initialized");
            
            // start it if autoStart is set
            if (config.autoStart)
                startModule(module);
            
            return module;
        }
        catch (SensorHubException e)
        {
            log.error("Error while initializing module " + config.name, e);
            throw e;
        }
        catch (Exception e)
        {
            throw new SensorHubException("Cannot load module " + config.name, e);
        }
    }
    
    
    /**
     * Loads the module with the given id<br/>
     * This method is asynchronous so it returns immediately
     * @param config Config of module to start
     * @return A Future whose result is available when the module is actually loaded
     */
    public Future<IModule<?>> loadModuleAsync(final ModuleConfig config)
    {
        Callable<IModule<?>> c = new Callable<IModule<?>>()
        {
            @Override
            public IModule<?> call() throws Exception
            {
                IModule<?> m = loadModule(config);
                return m;
            }            
        };
        
        return asyncExec.submit(c);
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
    public synchronized void unloadModule(String moduleID) throws SensorHubException
    {
        stopModule(moduleID);        
        IModule<?> module = loadedModules.remove(moduleID);
        eventHandler.publishEvent(new ModuleEvent(module, ModuleState.UNLOADED));        
        log.debug("Module " + MsgUtils.moduleString(module) +  " unloaded");
    }
    
    
    /*
     * Infers module dependencies by scanning all String properties
     * for a UUID existing in the module database
     */
    /*private String[] getReferencedModules(Object config)
    {
        List<String> refdModuleIDs = new ArrayList<String>();
        
        try
        {   
            for (Field f: config.getClass().getFields())
            {
                if (f.getDeclaringClass().equals(String.class))
                {
                    String text = (String)f.get(config);
                    if (configRepos.contains(text))
                        refdModuleIDs.add(text);
                }
            }
        }
        catch (Exception e)
        {            
        }
        
        return refdModuleIDs.toArray(new String[0]);
    }*/
    
    
    /**
     * Starts the module with the given local ID<br/>
     * This method is synchronous so it will block until the module is actually started
     * or an exception is thrown
     * @param moduleID Local ID of module to enable
     * @return module instance corresponding to moduleID
     * @throws SensorHubException 
     */
    public IModule<?> startModule(String moduleID) throws SensorHubException
    {
        try
        {
            checkID(moduleID);        
            IModule<?> module = loadedModules.get(moduleID);
            
            // load module if needed
            if (module == null)
            {
                ModuleConfig config = configRepos.get(moduleID);
                return loadModule(config);
            }
            
            //  start it if not already started
            if (!module.isStarted())
                startModule(module);
            
            return module;
        }
        catch (SensorHubException e)
        {
            log.error("Error while starting module " + moduleID, e);
            throw e;
        }
    }
    
    
    protected IModule<?> startModule(IModule<?> module) throws SensorHubException
    {
        // set STARTING state
        setModuleState(module, ModuleState.STARTING);
        log.trace("Module " + MsgUtils.moduleString(module) +  " starting");
                        
        // call module start method
        module.start();
        
        // set STARTED state
        setModuleState(module, ModuleState.STARTED);
        log.debug("Module " + MsgUtils.moduleString(module) +  " started");
            
        return module;
    }
    
    
    /**
     * Starts the module with the given id<br/>
     * This method is asynchronous so it returns immediately
     * @param moduleID Local ID of module to start
     * @return A Future whose result is available when the module is actually started
     */
    public Future<IModule<?>> startModuleAsync(final String moduleID)
    {
        Callable<IModule<?>> c = new Callable<IModule<?>>()
        {
            @Override
            public IModule<?> call() throws Exception
            {
                IModule<?> m = startModule(moduleID);
                return m;
            }            
        };
        
        return asyncExec.submit(c);
    }
    
    
    /**
     * Stops the module with the given local ID<br/>
     * This method is synchronous so it will block until the module is actually stopped
     * or an exception is thrown
     * @param moduleID Local ID of module to disable
     * @return module instance corresponding to moduleID
     * @throws SensorHubException 
     */
    public IModule<?> stopModule(String moduleID) throws SensorHubException
    {
        try
        {
            checkID(moduleID);
                    
            // stop module if it is loaded and started
            IModule<?> module = loadedModules.get(moduleID);
            if (module != null && module.isStarted())
            {
                try
                {
                    // set STOPPING state
                    setModuleState(module, ModuleState.STOPPING);
                    log.trace("Module " + MsgUtils.moduleString(module) +  " stopping");
                    
                    // call module stop method
                    module.stop();
                    
                    // set STOPPED state
                    setModuleState(module, ModuleState.STOPPED);                    
                    log.debug("Module " + MsgUtils.moduleString(module) + " stopped");
                }
                catch (Exception e)
                {
                    throw new SensorHubException("Error while stopping module " + MsgUtils.moduleString(module), e);
                }
            }
            
            return module;
        }
        catch (SensorHubException e)
        {
            log.error("Error while stopping module " + moduleID, e);
            throw e;
        }
    }
    
    
    /**
     * Stops the module with the given id<br/>
     * This method is asynchronous so it returns immediately
     * @param moduleID Local ID of module to stop
     * @return A Future whose result is available when the module is actually stopped
     */
    public Future<IModule<?>> stopModuleAsync(final String moduleID)
    {
        Callable<IModule<?>> c = new Callable<IModule<?>>()
        {
            @Override
            public IModule<?> call() throws Exception
            {
                IModule<?> m = stopModule(moduleID);
                return m;
            }            
        };
        
        return asyncExec.submit(c);
    }
    
    
    protected void setModuleState(IModule<?> module, ModuleState newState)
    {
        if (module instanceof AbstractModule)
            ((AbstractModule<?>)module).setState(newState);
    }
    
    
    /**
     * Removes the module with the given id
     * @param moduleID Local ID of module to delete
     * @throws SensorHubException 
     */
    public synchronized void destroyModule(String moduleID) throws SensorHubException
    {
        checkID(moduleID);
        
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
        
        eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.ModuleState.DELETED));
        log.debug("Module " + MsgUtils.moduleString(module) +  " removed");
    }
    
    
    /**
     * Save all modules current configuration to the repository
     */
    public synchronized void saveModulesConfiguration()
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
    
    
    /**
     * Saves the given module configurations in the repository
     * @param configList 
     */
    public synchronized void saveConfiguration(ModuleConfig... configList)
    {
        for (ModuleConfig config: configList)
            configRepos.update(config);
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.sensorhub.api.module.IModuleManager#getLoadedModules()
     */
    @Override
    public synchronized Collection<IModule<?>> getLoadedModules()
    {
        return Collections.unmodifiableCollection(loadedModules.values());
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.sensorhub.api.module.IModuleManager#getModuleById(java.lang.String)
     */
    @Override
    public IModule<?> getModuleById(String moduleID) throws SensorHubException
    {
        // load module if necessary
        if (!loadedModules.containsKey(moduleID))
        {
            if (configRepos.contains(moduleID))
                loadModule(configRepos.get(moduleID));
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
    
    
    /*
     * (non-Javadoc)
     * @see org.sensorhub.api.module.IModuleManager#getAvailableModules()
     */
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
        // stop all modules
        for (IModule<?> module: getLoadedModules())
        {
            try
            {
                // save config if requested
                if (saveConfig)
                    configRepos.update(module.getConfiguration());
                
                // cleanly stop module
                this.stopModule(module.getLocalID());
                
                // save state if requested
                if (saveState)
                {
                    try
                    {                   
                        module.saveState(getStateManager(module.getLocalID()));
                    }
                    catch (Exception e)
                    {
                        log.warn("Module state not saved", e);
                    }
                }
            }
            catch (Exception e)
            {
                log.error("Error during shutdown", e);
            }
        }
        
        loadedModules.clear();
        
        // make sure to clear all listeners in case some modules failed to unregister themselves
        eventHandler.clearAllListeners();
        
        // properly close config database
        configRepos.close();
    }
    
    
    /*
     * Checks if module id exists in registry
     */
    private void checkID(String moduleID)
    {
        // moduleID can exist either in live table, in config repository or both
        if (!loadedModules.containsKey(moduleID) && !configRepos.contains(moduleID))
            throw new RuntimeException("Module with ID " + moduleID + " is not available");
    }
    
    
    private IModuleStateManager getStateManager(String localID)
    {
        return new DefaultModuleStateManager(localID);
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
        // forward all lifecycle events from modules loaded by this registry
        if (e instanceof ModuleEvent)
            eventHandler.publishEvent(e);
    }

}
