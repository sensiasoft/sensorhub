/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.sensor;

import java.util.List;
import java.util.Map;
import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.common.IEventProducer;
import org.sensorhub.api.data.IDataProducerModule;


/**
 * <p>
 * Interface to be implemented by all sensor drivers connected to the system
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ConfigType> 
 * @since Nov 5, 2010
 */
public interface ISensorModule<ConfigType extends SensorConfig> extends IDataProducerModule<ConfigType>, IEventProducer
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
     * Retrieves most current sensor description.
     */
    @Override
    public AbstractProcess getCurrentDescription();


    /**
     * Used to check when sensor description was last updated.
     */
    @Override
    public double getLastDescriptionUpdate();


    /**
     * Retrieves historic sensor description valid at time t
     * @param time julian time (1970) at which description is valid
     * @return SMLSytem object containing sensor metadata valid at time t
     * @throws SensorException 
     */
    public AbstractProcess getSensorDescription(double time) throws SensorException;
    
    
    /**
     * Gets the whole history of sensor descriptions
     * @return list of process descriptions (with disjoint time validity periods)
     * @throws SensorException
     */
    public List<AbstractProcess> getSensorDescriptionHistory() throws SensorException;


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
     */
    @Override
    public Map<String, ? extends ISensorDataInterface> getAllOutputs();


    /**
     * Retrieves the list of interface to sensor status outputs
     * @return map of output names -> data interface objects
     */
    public Map<String, ? extends ISensorDataInterface> getStatusOutputs();


    /**
     * Retrieves the list of interface to sensor observation outputs
     * @return map of output names -> data interface objects
     */
    public Map<String, ? extends ISensorDataInterface> getObservationOutputs();


    /**
     * Retrieves the list of interface to sensor command inputs
     * @return map of input names -> control interface objects
     */
    public Map<String, ? extends ISensorControlInterface> getCommandInputs();


    /**
     * Returns the sensor connection status.<br/>
     * This method must do whatever it can to really detect the presence of the sensor. Consequently,
     * the method can take a long time to complete and must not be called repeatedly.
     * @return true if sensor is actually connected and can communicate with the driver
     */
    public boolean isConnected();

}