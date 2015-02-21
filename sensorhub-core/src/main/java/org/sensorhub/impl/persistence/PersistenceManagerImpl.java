/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IPersistenceManager;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Default implementation of the persistence manager. 
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 15, 2010
 */
public class PersistenceManagerImpl implements IPersistenceManager
{
    private static final Logger log = LoggerFactory.getLogger(PersistenceManagerImpl.class);    
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
            throw new SensorHubException("Module " + MsgUtils.moduleString(module) + " is not a storage module");
    }
    
    
    @Override
    public List<IBasicStorage<?>> findStorageForSensor(String sensorLocalID) throws SensorHubException
    {
        List<IBasicStorage<?>> sensorStorageList = new ArrayList<IBasicStorage<?>>();
        
        ISensorModule<?> sensorModule = SensorHub.getInstance().getSensorManager().getModuleById(sensorLocalID);
        String sensorUID = sensorModule.getCurrentSensorDescription().getUniqueIdentifier();
        
        // find all basic storage modules whose data source UID is the same as the sensor UID
        List<IStorageModule<?>> storageModules = getLoadedModules();
        for (IStorageModule<?> module: storageModules)
        {
            if (module instanceof IBasicStorage)
            {
                String dataSourceUID = ((IBasicStorage<?>) module).getLatestDataSourceDescription().getUniqueIdentifier();
                
                if (dataSourceUID != null && dataSourceUID.equals(sensorUID))
                    sensorStorageList.add((IBasicStorage<?>)module);
            }
        }
        
        return sensorStorageList;
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
