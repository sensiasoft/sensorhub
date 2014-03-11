/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.IPersistenceManager;
import org.sensorhub.api.persistence.ISensorDescriptionStorage;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.module.ModuleRegistry;


/**
 * <p>
 * Default implementation of the persistence manager. 
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 15, 2010
 */
public class PersistenceManagerImpl implements IPersistenceManager
{
    private static final Log log = LogFactory.getLog(PersistenceManagerImpl.class);    
    protected ModuleRegistry moduleRegistry;
    protected String basePath;
    
    
    public PersistenceManagerImpl(ModuleRegistry moduleRegistry, String basePath)
    {
        this.moduleRegistry = moduleRegistry;
        this.basePath = basePath;
    }
    
    
    @Override
    public List<IStorageModule<?>> getLoadedModules()
    {
        List<IStorageModule<?>> enabledStorages = new ArrayList<IStorageModule<?>>();
        
        // retrieve all modules implementing ISensorInterface
        for (IModule<?> module: moduleRegistry.getLoadedModules())
        {
            if (module instanceof IStorageModule)
                enabledStorages.add((IStorageModule<?>)module);
        }
        
        return enabledStorages;
    }


    @Override
    public List<ModuleConfig> getAvailableModules()
    {
        List<ModuleConfig> storageTypes = new ArrayList<ModuleConfig>();
        
        // retrieve all modules implementing IStorageModule
        for (ModuleConfig config: moduleRegistry.getAvailableModules())
        {
            try
            {
                if (IStorageModule.class.isAssignableFrom(Class.forName(config.moduleClass)))
                    storageTypes.add(config);
            }
            catch (Exception e)
            {
            }
        }
        
        return storageTypes;
    }


    @Override
    public IStorageModule<?> getModuleById(String moduleID) throws SensorHubException
    {
        IModule<?> module = moduleRegistry.getModuleById(moduleID);
        
        if (module instanceof IStorageModule<?>)
            return (IStorageModule<?>)module;
        else
            return null;
    }


    @Override
    public ISensorDescriptionStorage<?> getSensorDescriptionStorage() throws SensorHubException
    {
        List<ModuleConfig> storageModules = getAvailableModules();
        for (ModuleConfig config: storageModules)
        {
            try
            {
                if (ISensorDescriptionStorage.class.isAssignableFrom(Class.forName(config.moduleClass)))
                    return (ISensorDescriptionStorage<?>)getModuleById(config.id);
            }
            catch (ClassNotFoundException e)
            {
            }
        }
        
        throw new StorageException("No sensor description storage available");
    }
    
    
    @Override
    public StorageConfig getDefaultStorageConfig(Class<?> storageClass) throws SensorHubException
    {
        List<IModuleProvider> storageModules = moduleRegistry.getInstalledModuleTypes();
        for (IModuleProvider provider: storageModules)
        {
            try
            {
                Class<?> moduleClass = provider.getModuleClass();
                if (storageClass.isAssignableFrom(moduleClass))
                {
                    StorageConfig newConfig = (StorageConfig)provider.getModuleConfigClass().newInstance();
                    newConfig.moduleClass = moduleClass.getCanonicalName();
                    newConfig.storagePath = basePath + File.separatorChar;
                    return newConfig;
                }
            }
            catch (Exception e)
            {
                log.error("Invalid configuration for module ", e);
            }
        }
        
        throw new StorageException("No persistent storage of type " + storageClass.getSimpleName() + " available");
    }

}
