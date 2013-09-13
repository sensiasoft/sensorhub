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

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.IDataStorage;
import org.sensorhub.api.persistence.IPersistenceManager;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.api.sensor.ISensorInterface;
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
    
    
    public PersistenceManagerImpl()
    {

    }
    
    
    @Override
    public List<IDataStorage<?,?,?>> getLoadedModules()
    {
        List<IDataStorage<?,?,?>> enabledStorages = new ArrayList<IDataStorage<?,?,?>>();
        
        // retrieve all modules implementing ISensorInterface
        for (IModule<?> module: ModuleRegistry.getInstance().getLoadedModules())
        {
            if (module instanceof ISensorInterface)
                enabledStorages.add((IDataStorage<?,?,?>)module);
        }
        
        return enabledStorages;
    }


    @Override
    public List<ModuleConfig> getAvailableModules()
    {
        List<ModuleConfig> configuredSensors = new ArrayList<ModuleConfig>();
        
        // retrieve all modules implementing ISensorInterface
        for (ModuleConfig config: ModuleRegistry.getInstance().getAvailableModules())
        {
            try
            {
                if (Class.forName(config.moduleClass).isInstance(ISensorInterface.class))
                    configuredSensors.add(config);
            }
            catch (Exception e)
            {
            }
        }
        
        return configuredSensors;
    }


    @Override
    public IDataStorage<?,?,?> getModuleById(String moduleID)
    {
        IModule<?> module = ModuleRegistry.getInstance().getModuleById(moduleID);
        
        if (module instanceof IDataStorage<?,?,?>)
            return (IDataStorage<?,?,?>)module;
        else
            return null;
    }


    @Override
    public void destroyStorage(String storageId, boolean deleteAllData) throws StorageException
    {
        IDataStorage<?,?,?> storage = (IDataStorage<?,?,?>)ModuleRegistry.getInstance().getModuleById(storageId);
        storage.close();
        if (deleteAllData)
            storage.cleanup();       
    }  
}
