/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.IEventProducer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.module.IModuleManager;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.common.BasicEventHandler;
import org.sensorhub.utils.MsgUtils;



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
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin
 * @since Sep 2, 2013
 */
public class ModuleRegistry implements IModuleManager<IModule<?>>, IEventProducer
{
    private static final Log log = LogFactory.getLog(ModuleRegistry.class);    
    
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
            Class<IModule> clazz = (Class<IModule>)Class.forName(config.moduleClass);
            IModule module = clazz.newInstance();
            module.init(config);
                        
            // keep track of what modules are loaded
            loadedModules.put(config.id, module);
            
            // send event
            eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.LOADED));
            
            // start it if enabled by default
            if (config.enabled)
            {
                module.start();
                eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.ENABLED));
            }
            
            return module;
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error while initializing module " + config.name, e);
        }
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
     * Enables the module with the given id
     * @param moduleID Local ID of module to enable
     * @return enabled module instance
     * @throws SensorHubException 
     */
    @SuppressWarnings("rawtypes")
    public synchronized IModule<?> enableModule(String moduleID) throws SensorHubException
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
            eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.ENABLED));
        }
        
        return module;
    }
    
    
    /**
     * Disables the module with the given id
     * @param moduleID Local ID of module to disable
     * @throws SensorHubException 
     */
    public synchronized void disableModule(String moduleID) throws SensorHubException
    {
        checkID(moduleID);
                
        ModuleConfig config = configRepos.get(moduleID);
        config.enabled = false;
        
        // also unload and stop module if it was loaded
        IModule<?> module = loadedModules.remove(moduleID);
        if (module != null)
        {
            try
            {
                module.stop();
            }
            catch (Exception e)
            {
                throw new SensorHubException("Error while stopping module " + MsgUtils.moduleString(module), e);
            }
            finally
            {
                eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.DISABLED));
            }
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
        }
        
        configRepos.remove(moduleID);        
        eventHandler.publishEvent(new ModuleEvent(module, ModuleEvent.Type.DELETED));
    }
    
    
    /**
     * Save all modules current configuration to the repository
     */
    public synchronized void saveModulesConfiguration()
    {
        for (IModule<?> module: loadedModules.values())
            saveConfiguration(module);
    }
    
    
    /**
     * Saves the module configuration in the repository
     * @param module 
     */
    public synchronized void saveConfiguration(IModule<?> module)
    {
        if (configRepos.contains(module.getLocalID()))
            configRepos.update(module.getConfiguration());
        else
            configRepos.add(module.getConfiguration());
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.sensorhub.api.module.IModuleManager#getLoadedModules()
     */
    @Override
    public synchronized List<IModule<?>> getLoadedModules()
    {
        List<IModule<?>> moduleList = new ArrayList<IModule<?>>();
        moduleList.addAll(loadedModules.values());
        return moduleList;
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
    
    
    /*
     * (non-Javadoc)
     * @see org.sensorhub.api.module.IModuleManager#getAvailableModules()
     */
    @Override
    public synchronized List<ModuleConfig> getAvailableModules()
    {
        return configRepos.getAllModulesConfigurations();
    }
    
    
    /**
     * Retrieves list of all installed module types
     * @return list of module providers (not the module themselves)
     */
    public List<IModuleProvider> getInstalledModuleTypes()
    {
        List<IModuleProvider> installedModules = new ArrayList<IModuleProvider>();
        
        ServiceLoader<IModuleProvider> sl = ServiceLoader.load(IModuleProvider.class);
        for (IModuleProvider provider: sl)
            installedModules.add(provider);
        
        return installedModules;
    }
    
    
    /**
     * Retrieves list of all installed module types that are sub-types
     * of the specified class
     * @param moduleClass Parent class of modules to search for
     * @return list of module providers (not the module themselves)
     */
    public List<IModuleProvider> getInstalledModuleTypes(Class<?> moduleClass)
    {
        List<IModuleProvider> installedModules = getInstalledModuleTypes();

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
                // save state if requested
                // TODO use state saver
                if (saveState)
                    module.saveState(null);
                
                // save config if requested
                if (saveConfig)
                    configRepos.update(module.getConfiguration());
                
                // cleanly stop module
                this.disableModule(module.getLocalID());
            }
            catch (Exception e)
            {
                log.error(e);
            }
        }
        
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
