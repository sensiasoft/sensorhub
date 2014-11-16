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

package org.sensorhub.api.sensor;

import java.util.Map;
import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.common.IEventProducer;
import org.sensorhub.api.module.IModule;
import org.vast.util.DateTime;


/**
 * <p>
 * Interface to be implemented by all sensor drivers connected to the system
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @param <ConfigType> 
 * @since Nov 5, 2010
 */
public interface ISensorModule<ConfigType extends SensorConfig> extends IModule<ConfigType>, IEventProducer
{
    /**
     * Checks sensor description update capability
     * If true, the updateSensorDescription method should be implemented
     * @return true if sensor description updates is supported, false otherwise
     */
    public boolean isSensorDescriptionUpdateSupported();
    
    
    /**
     * Checks sensor description history capability
     * If true, the getSensorDescription(DateTime t) method should be implemented
     * @return true if sensor description history is supported, false otherwise
     */
    public boolean isSensorDescriptionHistorySupported();
    
    
    /**
	 * Retrieves most current sensor description
	 * @return SMLSytem object containing all metadata about the sensor
     * @throws SensorException 
	 */
	public AbstractProcess getCurrentSensorDescription() throws SensorException;
	
	
	/**
	 * Used to check when sensor description was last updated.
	 * This is useful to avoid requesting the object when it hasn't changed.
	 * @return date/time of last sensor description update 
	 */
	public long getLastSensorDescriptionUpdate();
	
	
	/**
	 * Retrieves historic sensor description valid at time t
	 * @param t
	 * @return SMLSytem object containing sensor metadata valid at time t
	 * @throws SensorException 
	 */
	public AbstractProcess getSensorDescription(DateTime t) throws SensorException;
	
	
	/**
	 * Updates and historizes system description
	 * @param systemDesc SMLSystem object with validity period
	 * @param recordHistory if true, older versions of the descriptions will be retained
	 * and made accessible by time
	 * @throws SensorException 
	 */
	public void updateSensorDescription(AbstractProcess systemDesc, boolean recordHistory) throws SensorException;
	
	
	/**
	 * Retrieves the list of interfaces to all sensor data outputs
	 * @return map of output names -> data interface objects
	 * @throws SensorException 
	 */
	public Map<String, ? extends ISensorDataInterface> getAllOutputs() throws SensorException;
	
	
	/**
	 * Retrieves the list of interface to sensor status outputs
	 * @return map of output names -> data interface objects
	 * @throws SensorException 
	 */
	public Map<String, ? extends ISensorDataInterface> getStatusOutputs() throws SensorException;
	
	
	/**
	 * Retrieves the list of interface to sensor observation outputs
	 * @return map of output names -> data interface objects
	 * @throws SensorException 
	 */
	public Map<String, ? extends ISensorDataInterface> getObservationOutputs() throws SensorException;
	
	
	/**
	 * Retrieves the list of interface to sensor command inputs
	 * @return map of input names -> control interface objects
	 * @throws SensorException 
	 */
	public Map<String, ? extends ISensorControlInterface> getCommandInputs() throws SensorException;
	
	
	/**
	 * Returns the sensor connection status
	 * @return true if sensor is actually connected and can communicate with the driver
	 */
	public boolean isConnected();
	
}