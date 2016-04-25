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
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.SensorSystemConfig.SensorMember;


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
    Map<String, ISensorModule<?>> sensors;
    Map<String, IProcessModule<?>> processes;
    
    
    @Override
    public void init(SensorSystemConfig config) throws SensorHubException
    {
        super.init(config);
        
        // generate XML ID
        this.xmlID.replace(DEFAULT_ID, "SYSTEM_");
                
        // load all sensor modules        
        sensors = new LinkedHashMap<String, ISensorModule<?>>();
        for (SensorMember member: config.sensors)
        {
            ISensorModule<SensorConfig> sensor = (ISensorModule<SensorConfig>)loadModule(member.config);
            sensors.put(member.name, sensor);
        }
        
        // TODO load all processing modules
        processes = new LinkedHashMap<String, IProcessModule<?>>();        
                
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
    
    
    private IModule<?> loadModule(ModuleConfig config) throws SensorException
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
            throw new SensorException("Cannot initialize system component " + config.name +
                                      " (" + config.moduleClass + ")", e);
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
            sensor.start();

        for (IProcessModule<?> process: processes.values())
            process.start();
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


    public Map<String, ISensorModule<?>> getSensors()
    {
        return sensors;
    }


    public Map<String, IProcessModule<?>> getProcesses()
    {
        return processes;
    }

}
