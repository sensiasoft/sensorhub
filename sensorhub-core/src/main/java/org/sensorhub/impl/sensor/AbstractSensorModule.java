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
 
 Please Contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.module.AbstractModule;
import org.vast.sensorML.SMLProcess;
import org.vast.sensorML.system.SMLSystem;
import org.vast.util.DateTime;


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
    protected SMLSystem sensorDescription;
    protected Map<String, ISensorDataInterface> obsOutputs = new HashMap<String, ISensorDataInterface>();  
    protected Map<String, ISensorDataInterface> statusOutputs = new HashMap<String, ISensorDataInterface>();  
    protected Map<String, ISensorControlInterface> controlInputs = new HashMap<String, ISensorControlInterface>();  
    
    
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
    public SMLProcess getCurrentSensorDescription() throws SensorException
    {
        return sensorDescription;
    }


    @Override
    public SMLProcess getSensorDescription(DateTime t) throws SensorException
    {
        throw new SensorException(ERROR_NO_HISTORY + getName());
    }


    @Override
    public void updateSensorDescription(SMLProcess systemDesc, boolean recordHistory) throws SensorException
    {
        throw new SensorException(ERROR_NO_UPDATE + getName());
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
