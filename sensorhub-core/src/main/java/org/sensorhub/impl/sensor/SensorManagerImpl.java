/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.ISensorManager;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.module.ModuleRegistry;


/**
 * <p>
 * Default implementation of the sensor manager interface
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class SensorManagerImpl implements ISensorManager
{
    protected ModuleRegistry moduleRegistry;
    
    
    public SensorManagerImpl(ModuleRegistry moduleRegistry)
    {
        this.moduleRegistry = moduleRegistry;
    }
    
    
    @Override
    public List<ISensorModule<?>> getLoadedModules()
    {
        List<ISensorModule<?>> enabledSensors = new ArrayList<ISensorModule<?>>();
        
        // retrieve all modules implementing ISensorInterface
        for (IModule<?> module: moduleRegistry.getLoadedModules())
        {
            if (module instanceof ISensorModule)
                enabledSensors.add((ISensorModule<?>)module);
        }
        
        return enabledSensors;
    }


    @Override
    public List<ModuleConfig> getAvailableModules()
    {
        List<ModuleConfig> configuredSensors = new ArrayList<ModuleConfig>();
        
        // retrieve all modules implementing ISensorInterface
        for (ModuleConfig config: moduleRegistry.getAvailableModules())
        {
            try
            {
                if (ISensorModule.class.isAssignableFrom(Class.forName(config.moduleClass)))
                    configuredSensors.add(config);
            }
            catch (Exception e)
            {
            }
        }
        
        return configuredSensors;
    }


    @Override
    public ISensorModule<?> getModuleById(String moduleID) throws SensorHubException
    {
        IModule<?> module = moduleRegistry.getModuleById(moduleID);
        
        if (module instanceof ISensorModule<?>)
            return (ISensorModule<?>)module;
        else
            return null;
    }


    @Override
    public ISensorModule<?> findSensor(String uid)
    {
        List<ISensorModule<?>> enabledSensors = getLoadedModules();
        for (ISensorModule<?> sensor: enabledSensors)
        {
            try
            {
                if (uid.equals(sensor.getCurrentDescription().getIdentifier()))
                    return sensor;
            }
            catch (SensorException e)
            {
            }
        }
        
        return null;
    }


    @Override
    public List<ISensorModule<?>> getConnectedSensors()
    {
        List<ISensorModule<?>> connectedSensors = new ArrayList<ISensorModule<?>>();
        
        // scan module list
        for (IModule<?> module: moduleRegistry.getLoadedModules())
        {
            if (module instanceof ISensorModule && ((ISensorModule<?>)module).isConnected())
                connectedSensors.add((ISensorModule<?>)module);
        }
        
        return connectedSensors;
    }


    @Override
    public String installDriver(String driverPackageURL, boolean replace)
    {
        // TODO Auto-generated method stub
        // TODO need to implement generic module software loading in ModuleRegistry
        //  + dynamic classloader handling new uploaded jars (scanning directory at startup)
        return null;
    }


    @Override
    public void uninstallDriver(String driverID)
    {
        // TODO Auto-generated method stub
    }


    @Override
    public List<IModuleProvider> getInstalledSensorDrivers()
    {
        List<IModuleProvider> installedModules = new ArrayList<IModuleProvider>();
        
        // retrieve all modules implementing ISensorInterface
        for (IModuleProvider modType: moduleRegistry.getInstalledModuleTypes())
        {
            try
            {
                if (modType.getModuleClass().isInstance(ISensorModule.class))
                    installedModules.add(modType);
            }
            catch (Exception e)
            {
            }
        }
        
        return installedModules;
    }

}
