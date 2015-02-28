/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataBlockMixed;
import org.vast.data.SWEFactory;
import org.vast.data.ScalarIterator;
import org.vast.ows.sps.DescribeTaskingResponse;
import org.vast.ows.sps.SPSOfferingCapabilities;
import org.vast.ows.swe.SWESOfferingCapabilities;
import org.vast.swe.SWEConstants;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * SPS connector for directly sending commands to sensors.
 * This connector doesn't support scheduling or persistent task management
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 13, 2014
 */
public class DirectSensorConnector implements ISPSConnector
{
    private static final Logger log = LoggerFactory.getLogger(DirectSensorConnector.class);
    
    final SensorConnectorConfig config;
    final ISensorModule<?> sensor;
    DataChoice commandChoice;
    String uniqueInterfaceName;
    
    
    public DirectSensorConnector(SensorConnectorConfig config) throws SensorHubException
    {
        this.config = config;
        
        // get handle to sensor instance using sensor manager
        this.sensor = SensorHub.getInstance().getSensorManager().getModuleById(config.sensorID);
    }
    

    @Override
    public SPSOfferingCapabilities generateCapabilities() throws ServiceException
    {
        checkEnabled();
        
        try
        {
            SPSOfferingCapabilities caps = new SPSOfferingCapabilities();
            
            // identifier
            if (config.uri != null)
                caps.setIdentifier(config.uri);
            else
                caps.setIdentifier("baseURL#" + sensor.getLocalID()); // TODO obtain baseURL
            
            // name
            if (config.name != null)
                caps.setTitle(config.name);
            else
                caps.setTitle(sensor.getName());
            
            // description
            if (config.description != null)
                caps.setDescription(config.description);
            else
                caps.setDescription("Tasking interface for " + sensor.getName());
            
            // use sensor uniqueID as procedure ID
            caps.getProcedures().add(sensor.getCurrentDescription().getUniqueIdentifier());
            
            // supported formats
            caps.getProcedureFormats().add(SWESOfferingCapabilities.FORMAT_SML2);
            
            // observable properties
            List<String> sensorOutputDefs = getObservablePropertiesFromSensor();
            caps.getObservableProperties().addAll(sensorOutputDefs);
            
            // TODO observable area = from manual config
                        
            // tasking parameters description
            DescribeTaskingResponse descTaskingResp = new DescribeTaskingResponse();
            List<DataComponent> commands = getCommandsFromSensor();
            if (commands.size() == 1)
            {
                descTaskingResp.setTaskingParameters(commands.get(0).copy());
                uniqueInterfaceName = commands.get(0).getName();
            }
            else
            {
                commandChoice = new SWEFactory().newDataChoice();
                for (DataComponent command: commands)
                    commandChoice.addItem(command.getName(), command.copy());
                descTaskingResp.setTaskingParameters(commandChoice);
            }
            caps.setParametersDescription(descTaskingResp);
            
            return caps;
        }
        catch (SensorException e)
        {
            throw new ServiceException("Error while generating capabilities for sensor " + MsgUtils.moduleString(sensor), e);
        }
    }
    
    
    protected List<DataComponent> getCommandsFromSensor() throws SensorException
    {
        List<DataComponent> commands = new ArrayList<DataComponent>();
        
        // process sensor commands descriptions
        for (Entry<String, ? extends ISensorControlInterface> entry: sensor.getCommandInputs().entrySet())
        {
            // skip hidden commands
            if (config.hiddenCommands != null && config.hiddenCommands.contains(entry.getKey()))
                continue;
            
            commands.add(entry.getValue().getCommandDescription());
        }
        
        return commands;
    }
    
    
    protected List<String> getObservablePropertiesFromSensor() throws SensorException
    {
        List<String> observableUris = new ArrayList<String>();
        
        // process outputs descriptions
        for (Entry<String, ? extends ISensorDataInterface> entry: sensor.getAllOutputs().entrySet())
        {
            // iterate through all SWE components and add all definition URIs as observables
            // this way only composites with URI will get added
            ISensorDataInterface output = entry.getValue();
            ScalarIterator it = new ScalarIterator(output.getRecordDescription());
            while (it.hasNext())
            {
                String defUri = (String)it.next().getDefinition();
                if (defUri != null && !defUri.equals(SWEConstants.DEF_SAMPLING_TIME))
                    observableUris.add(defUri);
            }
        }
        
        return observableUris;
    }
    
    
    @Override
    public void updateCapabilities() throws Exception
    {
        // TODO Auto-generated method stub
        
    }
    
    
    @Override
    public AbstractProcess generateSensorMLDescription(double time) throws Exception
    {
        return sensor.getCurrentDescription();
    }


    @Override
    public void sendSubmitData(ITask task, DataBlock data) throws ServiceException
    {
        checkEnabled();
        ISensorControlInterface cmdInterface;
        
        try
        {
            // figure out which control interface to use
            if (commandChoice != null)
            {
                // select interface depending on choice token
                int selectedIndex = data.getIntValue(0);
                String itemName = commandChoice.getComponent(selectedIndex).getName();
                cmdInterface = sensor.getCommandInputs().get(itemName);
                data = ((DataBlockMixed)data).getUnderlyingObject()[1];
            }
            else
            {
                // use the sole interface
                cmdInterface = sensor.getCommandInputs().get(uniqueInterfaceName);
            }
            
            // actually send command to selected interface
            cmdInterface.execCommand(data);
        }
        catch (SensorException e)
        {
            String msg = "Error sending command to sensor " + MsgUtils.moduleString(sensor);
            log.error(msg, e);
            throw new ServiceException(msg, e);
        }
    }
    
    
    /*
     * Checks if provider and underlying sensor are enabled
     */
    protected void checkEnabled() throws ServiceException
    {
        if (!config.enabled)
        {
            String providerName = (config.name != null) ? config.name : "for " + config.sensorID;
            throw new ServiceException("Connector " + providerName + " is disabled");
        }
        
        if (!sensor.isEnabled())
            throw new ServiceException("Sensor " + MsgUtils.moduleString(sensor) + " is disabled");
    }
    
    
    @Override
    public void cleanup()
    {
        //SensorHub.getInstance().unregisterListener(this);
    }


    @Override
    public boolean isEnabled()
    {
        return (config.enabled && sensor.isEnabled());
    }
}
