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

package org.sensorhub.impl.sensor;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.sensor.ISensorInterface;
import org.sensorhub.api.sensor.ISensorManager;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.module.ModuleRegistry;


/**
 * <p>
 * Default implementation of the sensor manager interface
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
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
    public List<ISensorInterface<?>> getLoadedModules()
    {
        List<ISensorInterface<?>> enabledSensors = new ArrayList<ISensorInterface<?>>();
        
        // retrieve all modules implementing ISensorInterface
        for (IModule<?> module: moduleRegistry.getLoadedModules())
        {
            if (module instanceof ISensorInterface)
                enabledSensors.add((ISensorInterface<?>)module);
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
                if (ISensorInterface.class.isAssignableFrom(Class.forName(config.moduleClass)))
                    configuredSensors.add(config);
            }
            catch (Exception e)
            {
            }
        }
        
        return configuredSensors;
    }


    @Override
    public ISensorInterface<?> getModuleById(String moduleID) throws SensorHubException
    {
        IModule<?> module = moduleRegistry.getModuleById(moduleID);
        
        if (module instanceof ISensorInterface<?>)
            return (ISensorInterface<?>)module;
        else
            return null;
    }


    @Override
    public ISensorInterface<?> findSensor(String uid)
    {
        List<ISensorInterface<?>> enabledSensors = getLoadedModules();
        for (ISensorInterface<?> sensor: enabledSensors)
        {
            try
            {
                if (uid.equals(sensor.getCurrentSensorDescription().getIdentifier()))
                    return sensor;
            }
            catch (SensorException e)
            {
            }
        }
        
        return null;
    }


    @Override
    public List<ISensorInterface<?>> getConnectedSensors()
    {
        List<ISensorInterface<?>> connectedSensors = new ArrayList<ISensorInterface<?>>();
        
        // scan module list
        for (IModule<?> module: moduleRegistry.getLoadedModules())
        {
            if (module instanceof ISensorInterface && ((ISensorInterface<?>)module).isConnected())
                connectedSensors.add((ISensorInterface<?>)module);
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
                if (modType.getModuleClass().isInstance(ISensorInterface.class))
                    installedModules.add(modType);
            }
            catch (Exception e)
            {
            }
        }
        
        return installedModules;
    }

}
