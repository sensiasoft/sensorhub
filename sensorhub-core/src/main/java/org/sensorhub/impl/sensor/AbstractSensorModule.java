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

package org.sensorhub.impl.sensor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.utils.MsgUtils;
import org.vast.sensorML.PhysicalSystemImpl;
import org.vast.sensorML.SMLUtils;


/**
 * <p>
 * Class providing default implementation of common sensor API methods.
 * By default, sensor description history and updates are reported as unsupported.
 * </p>
 *
 * <p>Copyright (c) 2014</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @param <ConfigType> 
 * @since Oct 30, 2014
 */
public abstract class AbstractSensorModule<ConfigType extends SensorConfig> extends AbstractModule<ConfigType> implements ISensorModule<ConfigType>
{
    protected static String ERROR_NO_UPDATE = "Sensor Description update is not supported by driver ";
    protected static String ERROR_NO_HISTORY = "History of sensor description is not supported by driver ";
    private Map<String, ISensorDataInterface> obsOutputs = new HashMap<String, ISensorDataInterface>();  
    private Map<String, ISensorDataInterface> statusOutputs = new HashMap<String, ISensorDataInterface>();  
    private Map<String, ISensorControlInterface> controlInputs = new HashMap<String, ISensorControlInterface>();  
    protected AbstractProcess sensorDescription = new PhysicalSystemImpl();
    protected double lastUpdatedSensorDescription = Double.NEGATIVE_INFINITY;
    
    
    /**
     * Call this method to add each sensor observation or status output
     * @param dataInterface interface to add as sensor output
     * @param isStatus set to true when registering a status output
     */
    protected void addOutput(ISensorDataInterface dataInterface, boolean isStatus)
    {
        if (isStatus)
            statusOutputs.put(dataInterface.getName(), dataInterface);
        else
            obsOutputs.put(dataInterface.getName(), dataInterface);
    }
    
    
    /**
     * Call this method to add each sensor control input
     * @param controlInterface interface to add as sensor control input
     */
    protected void addControlInput(ISensorControlInterface controlInterface)
    {
        controlInputs.put(controlInterface.getName(), controlInterface);
    }
    
    
    @Override
    public boolean isSensorDescriptionUpdateSupported()
    {
        return false;
    }


    @Override
    public boolean isSensorDescriptionHistorySupported()
    {
        return false;
    }


    @Override
    public AbstractProcess getCurrentSensorDescription() throws SensorException
    {
        synchronized (sensorDescription)
        {
            if (lastUpdatedSensorDescription == Double.NEGATIVE_INFINITY)
                updateSensorDescription();
        
            return sensorDescription;
        }
    }
    
    
    /**
     * This method should be called whenever the sensor description needs to be regenerated.
     * This default implementation reads the base description from the SensorML file if provided
     * and then appends the unique sensor identifier as well as the description of all registered
     * outputs and control inputs. This will also update the lastUpdatedSensorDescription time stamp.
     * @throws SensorException
     */
    protected void updateSensorDescription() throws SensorException
    {
        synchronized (sensorDescription)
        {
            // by default we return the static description provided in config
            if (config.sensorML != null && config.sensorML.length() > 0)
            {
                try
                {
                    SMLUtils utils = new SMLUtils();
                    InputStream is = new URL(config.sensorML).openStream();
                    sensorDescription = utils.readProcess(is);
                }
                catch (IOException e)
                {
                    throw new SensorException("Error while parsing static SensorML description for sensor " +
                                                MsgUtils.moduleString(this), e);
                }
            }
            else
            {
                sensorDescription = new PhysicalSystemImpl();
            }
            
            // add most common stuff automatically
            sensorDescription.setUniqueIdentifier(getLocalID());    
            
            // append outputs only if not already defined in static doc
            if (sensorDescription.getNumOutputs() == 0)
            {
                for (Entry<String, ? extends ISensorDataInterface> output: getAllOutputs().entrySet())
                {
                    DataComponent outputDesc = output.getValue().getRecordDescription();
                    if (outputDesc == null)
                        continue;
                    outputDesc = outputDesc.copy();
                    sensorDescription.addOutput(output.getKey(), outputDesc);
                }
            }
            
            // append control parameters only if not already defined in static doc
            if (sensorDescription.getNumParameters() == 0)
            {
                for (Entry<String, ? extends ISensorControlInterface> param: getCommandInputs().entrySet())
                {
                    DataComponent paramDesc = param.getValue().getCommandDescription();
                    if (paramDesc == null)
                        continue;
                    paramDesc = paramDesc.copy();
                    paramDesc.setUpdatable(true);
                    sensorDescription.addParameter(param.getKey(), paramDesc);
                }
            }
            
            lastUpdatedSensorDescription = System.currentTimeMillis() / 1000.;
            eventHandler.publishEvent(new SensorEvent(lastUpdatedSensorDescription, getLocalID(), SensorEvent.Type.SENSOR_CHANGED));
        }
    }


    @Override
    public double getLastSensorDescriptionUpdate()
    {
        return lastUpdatedSensorDescription;
    }


    @Override
    public AbstractProcess getSensorDescription(double time) throws SensorException
    {
        throw new SensorException(ERROR_NO_HISTORY + MsgUtils.moduleClassAndId(this));
    }


    @Override
    public List<AbstractProcess> getSensorDescriptionHistory() throws SensorException
    {
        throw new SensorException(ERROR_NO_HISTORY + MsgUtils.moduleClassAndId(this));
    }


    @Override
    public void updateSensorDescription(AbstractProcess systemDesc, boolean recordHistory) throws SensorException
    {
        throw new SensorException(ERROR_NO_UPDATE + MsgUtils.moduleClassAndId(this));
    }


    @Override
    public Map<String, ? extends ISensorDataInterface> getAllOutputs() throws SensorException
    {
        Map<String, ISensorDataInterface> allOutputs = new HashMap<String, ISensorDataInterface>();  
        allOutputs.putAll(obsOutputs);
        allOutputs.putAll(statusOutputs);
        return Collections.unmodifiableMap(allOutputs);
    }


    @Override
    public Map<String, ? extends ISensorDataInterface> getStatusOutputs() throws SensorException
    {
        return Collections.unmodifiableMap(statusOutputs);
    }


    @Override
    public Map<String, ? extends ISensorDataInterface> getObservationOutputs() throws SensorException
    {
        return Collections.unmodifiableMap(obsOutputs);
    }


    @Override
    public Map<String, ? extends ISensorControlInterface> getCommandInputs() throws SensorException
    {
        return Collections.unmodifiableMap(controlInputs);
    }
}
