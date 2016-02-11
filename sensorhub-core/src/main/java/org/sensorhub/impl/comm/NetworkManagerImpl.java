/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm;

import java.util.ArrayList;
import java.util.Collection;
import org.sensorhub.api.comm.ICommNetwork;
import org.sensorhub.api.comm.INetworkManager;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.module.ModuleRegistry;


/**
 * <p>
 * Network manager allowing to list and configure all communication networks
 * available on the platform.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 7, 2016
 */
public class NetworkManagerImpl implements INetworkManager
{
    protected ModuleRegistry moduleRegistry;
    
    
    public NetworkManagerImpl(ModuleRegistry moduleRegistry)
    {
        this.moduleRegistry = moduleRegistry;
    }
    
    
    @Override
    public Collection<ICommNetwork<?>> getLoadedModules()
    {
        ArrayList<ICommNetwork<?>> enabledSensors = new ArrayList<ICommNetwork<?>>();
        
        // retrieve all modules implementing ISensorInterface
        for (IModule<?> module: moduleRegistry.getLoadedModules())
        {
            if (module instanceof ICommNetwork)
                enabledSensors.add((ICommNetwork<?>)module);
        }
        
        return enabledSensors;
    }
    
    
    @Override
    public boolean isModuleLoaded(String moduleID)
    {
        return moduleRegistry.isModuleLoaded(moduleID);
    }


    @Override
    public Collection<ModuleConfig> getAvailableModules()
    {
        ArrayList<ModuleConfig> configuredSensors = new ArrayList<ModuleConfig>();
        
        // retrieve all modules implementing ISensorInterface
        for (ModuleConfig config: moduleRegistry.getAvailableModules())
        {
            try
            {
                if (ICommNetwork.class.isAssignableFrom(Class.forName(config.moduleClass)))
                    configuredSensors.add(config);
            }
            catch (Exception e)
            {
            }
        }
        
        return configuredSensors;
    }


    @Override
    public ICommNetwork<?> getModuleById(String moduleID) throws SensorHubException
    {
        IModule<?> module = moduleRegistry.getModuleById(moduleID);
        
        if (module instanceof ICommNetwork)
            return (ICommNetwork<?>)module;
        else
            return null;
    }

}
