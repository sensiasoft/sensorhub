/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.SensorSystemConfig.ProcessMember;
import org.sensorhub.impl.sensor.SensorSystemConfig.SensorMember;
import org.sensorhub.utils.MsgUtils;


/**
 * <p>
 * Class allowing to group several sensors drivers and processes into a single
 * system.<br/>
 * The system's outputs consist of the ones from the individual sensors and
 * processes included in the group.<br/>
 * Relative location and orientation of components can also be set
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 19, 2016
 */
public class SensorSystem extends AbstractSensorModule<SensorSystemConfig>
{
    public final static String AUTO_ID = "auto";    
    private final static String HTTP_PREFIX = "http://";
    private final static String URN_PREFIX = "urn:";
    
    Map<String, ISensorModule<?>> sensors;
    Map<String, IProcessModule<?>> processes;
    
    
    @Override
    public void init(SensorSystemConfig config) throws SensorHubException
    {
        super.init(config);
        
        // generate XML ID
        this.xmlID.replace(DEFAULT_ID, "SYSTEM_");
        
        // set unique ID
        if (config.uniqueID != null && !config.uniqueID.equals(AUTO_ID))
        {
            if (config.uniqueID.startsWith(HTTP_PREFIX) || config.uniqueID.startsWith(URN_PREFIX))
                this.uniqueID = config.uniqueID;
            else
                this.uniqueID = URN_PREFIX + "osh:system:" + config.uniqueID;
        }
        
        // load all sensor modules        
        sensors = new LinkedHashMap<String, ISensorModule<?>>();
        for (SensorMember member: config.sensors)
        {
            ISensorModule<?> sensor = (ISensorModule<?>)loadModule(member.config);
            if (sensor != null)
                sensors.put(member.name, sensor);
        }
        
        // load all processing modules
        processes = new LinkedHashMap<String, IProcessModule<?>>();        
        for (ProcessMember member: config.processes)
        {
            IProcessModule<?> process = (IProcessModule<?>)loadModule(member.config);
            if (process != null)
                processes.put(member.name, process);
        }
        
        // aggregate all sensors outputs and control inputs
        for (ISensorModule<?> sensor: sensors.values())
        {
            for (ISensorDataInterface output: sensor.getObservationOutputs().values())
                this.addOutput(output, false);
            
            for (ISensorDataInterface output: sensor.getStatusOutputs().values())
                this.addOutput(output, true);
            
            for (ISensorControlInterface input: sensor.getCommandInputs().values())
                this.addControlInput(input);
        }
    }
    
    
    private IModule<?> loadModule(ModuleConfig config)
    {
        try
        {
            if (config.id == null)
                config.id = UUID.randomUUID().toString();
            
            Class<?> clazz = (Class<?>)Class.forName(config.moduleClass);
            IModule<ModuleConfig> module = (IModule<ModuleConfig>)clazz.newInstance();
            module.init(config);
            return module;
        }
        catch (Exception e)
        {
            getLogger().error("Cannot initialize system component {}", MsgUtils.moduleString(config), e);
            return null;
        }
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            PhysicalSystem system = (PhysicalSystem)sensorDescription;
            
            // include sensor descriptions as components
            for (Entry<String, ISensorModule<?>> entry: sensors.entrySet())
                system.addComponent(entry.getKey(), entry.getValue().getCurrentDescription());
        }
    }


    @Override
    public void start() throws SensorHubException
    {
        for (ISensorModule<?> sensor: sensors.values())
        {
            try
            {
                sensor.start();
                
                // add sensor outputs and control inputs now in case they didn't exist in init()
                for (ISensorDataInterface output: sensor.getObservationOutputs().values())
                    this.addOutput(output, false);
                
                for (ISensorDataInterface output: sensor.getStatusOutputs().values())
                    this.addOutput(output, true);
                
                for (ISensorControlInterface input: sensor.getCommandInputs().values())
                    this.addControlInput(input);
            }
            catch (Exception e)
            {
                getLogger().error("Cannot start system sensor {}", MsgUtils.moduleString(sensor), e);
            }
        }
        
        for (IProcessModule<?> process: processes.values())
        {
            try
            {
                process.start();
            }
            catch (Exception e)
            {
                getLogger().error("Cannot start system process {}", MsgUtils.moduleString(process), e);
            }
        }
    }


    @Override
    public void stop() throws SensorHubException
    {
        for (ISensorModule<?> sensor: sensors.values())
            sensor.stop();
        
        for (IProcessModule<?> process: processes.values())
            process.stop();
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        for (ISensorModule<?> sensor: sensors.values())
            sensor.cleanup();
        
        for (IProcessModule<?> process: processes.values())
            process.cleanup();
    }


    @Override
    public boolean isConnected()
    {
        for (ISensorModule<?> sensor: sensors.values())
        {
            if (!sensor.isConnected())
                return false;
        }
        
        return true;
    }


    @Override
    public void loadState(IModuleStateManager loader) throws SensorHubException
    {
        super.loadState(loader);
        
        // also load sub modules state
        ModuleRegistry reg = SensorHub.getInstance().getModuleRegistry();
        for (ISensorModule<?> sensor: sensors.values())
        {
            loader = reg.getStateManager(sensor.getLocalID());
            if (loader != null)
                sensor.loadState(loader);
        }
        
        for (IProcessModule<?> process: processes.values())
        {
            loader = reg.getStateManager(process.getLocalID());
            if (loader != null)
                process.loadState(loader);
        }
    }


    @Override
    public void saveState(IModuleStateManager saver) throws SensorHubException
    {
        super.saveState(saver);
        
        // also save sub modules state
        ModuleRegistry reg = SensorHub.getInstance().getModuleRegistry();
        for (ISensorModule<?> sensor: sensors.values())
        {
            saver = reg.getStateManager(sensor.getLocalID());
            sensor.saveState(saver);
        }
        
        for (IProcessModule<?> process: processes.values())
        {
            saver = reg.getStateManager(process.getLocalID());
            process.saveState(saver);
        }
    }


    public Map<String, ISensorModule<?>> getSensors()
    {
        return sensors;
    }


    public Map<String, IProcessModule<?>> getProcesses()
    {
        return processes;
    }

}
