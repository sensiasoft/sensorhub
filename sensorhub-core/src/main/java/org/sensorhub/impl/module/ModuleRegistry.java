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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
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
public class ModuleRegistry implements IModuleManager<IModule<?>>, IEventProducer
{
    private static final Logger log = LoggerFactory.getLogger(ModuleRegistry.class);    
    
    IModuleConfigRepository configRepos;
    Map<String, IModule<?>> loadedModules;
    IEventHandler eventHandler;
    
    
    public ModuleRegistry(IModuleConfigRepository configRepos)
    {
        this.configRepos = configRepos;
        this.loadedModules = new LinkedHashMap<String, IModule<?>>();        
        this.eventHandler = new BasicEventHandler();
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
    public synchronized IModule<?> loadModule(ModuleConfig config) throws SensorHubException
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
            
            // instantiate and init module class
            IModule module = (IModule)loadClass(config.moduleClass);
            module.init(config);
            
            // load saved module state
            module.loadState(getStateManager(config.id));
                        
            // keep track of what modules are loaded
            loadedModules.put(config.id, module);
                        
            // send event
            eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.LOADED));
            log.debug("Module " + MsgUtils.moduleString(module) +  " loaded");
            
            // start it if enabled by default
            if (config.enabled)
            {
                module.start();
                eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.ENABLED));
                log.debug("Module " + MsgUtils.moduleString(module) +  " started");
            }
            
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
    
    
    public Object loadClass(String className) throws SensorHubException
    {
        try
        {
            Class<?> clazz = (Class<?>)Class.forName(className);
            return clazz.newInstance();
        }
        catch (ClassNotFoundException | IllegalAccessException | InstantiationException e)
        {
            throw new SensorHubException("Cannot instantiate module class", e);
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
        disableModule(moduleID);        
        IModule<?> module = loadedModules.remove(moduleID);
        eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.UNLOADED));        
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
     * Enables/Start the module with the given id
     * @param moduleID Local ID of module to enable
     * @return enabled module instance
     * @throws SensorHubException 
     */
    @SuppressWarnings("rawtypes")
    public synchronized IModule<?> enableModule(String moduleID) throws SensorHubException
    {
        try
        {
            checkID(moduleID);        
            IModule module = loadedModules.get(moduleID);
            
            // load module if not already loaded
            if (module == null)
            {
                ModuleConfig config = configRepos.get(moduleID);
                config.enabled = true;
                module = loadModule(config);
            }
            
            // otherwise just start it
            else
            {
                module.start();      
                module.getConfiguration().enabled = true;
                eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.ENABLED));
                log.debug("Module " + MsgUtils.moduleString(module) +  " started");
            }
            
            return module;
        }
        catch (SensorHubException e)
        {
            log.error("Error while starting module " + moduleID, e);
            throw e;
        }
    }
    
    
    /**
     * Disables the module with the given id
     * @param moduleID Local ID of module to disable
     * @throws SensorHubException 
     */
    public synchronized void disableModule(String moduleID) throws SensorHubException
    {
        try
        {
            checkID(moduleID);
                    
            // stop module if it was loaded
            IModule<?> module = loadedModules.get(moduleID);
            if (module != null)
            {
                try
                {
                    module.stop();
                    module.getConfiguration().enabled = false;
                }
                catch (Exception e)
                {
                    throw new SensorHubException("Error while stopping module " + MsgUtils.moduleString(module), e);
                }
                
                eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.DISABLED));
                log.debug("Module " + MsgUtils.moduleString(module) + " stopped");
            }
        }
        catch (SensorHubException e)
        {
            log.error("Error while stopping module " + moduleID, e);
            throw e;
        }
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
        
        eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.DELETED));
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
        // start module if necessary
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
                this.disableModule(module.getLocalID());
                
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

}
