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

package org.sensorhub.api.sensor;

import java.util.List;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.common.IEventProducer;


/**
 * <p>
 * Interface to be implemented by all sensor drivers connected to the system
 * Data from each sensor output is made available through this interface.
 * Data can be observations or status information.
 * Implementations must at least be capable of retaining the latest record
 * received from sensor until the getLatestRecord is called.
 * If push is supported, implementations produce events of type SensorDataEvent.
 * </p>
 * 
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 5, 2010
 */
public interface ISensorDataInterface extends IEventProducer
{

    /**
     * Allows by-reference access to parent sensor module
     * @return parent sensor
     */
    public ISensorModule<?> getParentSensor();
    
    
    /**
     * Gets the interface name. It should be the name reported in the map by
     * getXXXOutputs methods of {@link org.sensorhub.api.sensor.ISensorModule} 
     * @see org.sensorhub.api.sensor.ISensorModule#getAllOutputs()
     * @return name of this output interface
     */
    public String getName();


    /**
     * Checks if this interface is enabled
     * @return true if interface is enabled, false otherwise
     */
    public boolean isEnabled();


    /**
     * Checks data storage capability
     * @return true if sensor or sensor driver supports observation storage natively, false otherwise
     */
    public boolean isStorageSupported();


    /**
     * Checks push capability
     * @return true if driver can send notifications when new data is available, false otherwise
     */
    public boolean isPushSupported();


    /**
     * Gets average sampling rate
     * @return sampling period in seconds
     */
    public double getAverageSamplingPeriod(); // used to know how often to poll


    /**
     * Retrieves the record definition for this output
     * @return a DataComponent object defining the structure of the output
     */
    public DataComponent getRecordDescription();


    /**
     * Provides the recommended encoding for this sensor data
     * @return recommending encoding description
     */
    public DataEncoding getRecommendedEncoding();


    //	/**
    //	 * Requests acquisition of a new measurement
    //	 * Measurement will be available via getLatestRecord until cleared or
    //	 * replaced by another measurement. It will also be added to storage if supported.
    //	 * Actual reception of the measurement by the driver triggers a NEW_DATA event.
    //	 * @return
    //	 * @throws SensorException
    //	 */
    //	public DataBlock requestNewRecord() throws SensorException;
    //  SHOULD BE IMPLEMENTED AS A COMMAND?

    /**
     * Gets the latest record from this data channel. Data is not removed from input buffer.
     * Even implementations with no storage support must always retain the last record.
     * @return the record as a DataBlock object or null if no data is available
     * @throws SensorException
     */
    public DataBlock getLatestRecord() throws SensorException;
    
    
    /**
     * Used to check when the last measurement was made. This is useful to know if a
     * measurement has really been made since the last call to {@link #getLatestRecord}.
     * @return time of last measurement as julian time (1970) or NaN if no measurement was made yet
     */
    public double getLatestRecordTime();


    /**
     * Gets storage capacity
     * @see #isStorageSupported()
     * @return maximum number of records that can be stored or 0 if storage is not supported
     * @throws SensorException
     */
    public int getStorageCapacity() throws SensorException;


    /**
     * Retrieves number of record currectly available from driver or sensor memory.
     * @see #isStorageSupported()
     * @return the number of available records
     * @throws SensorException if storage is not supported or a problem occured while reading storage
     */
    public int getNumberOfAvailableRecords() throws SensorException;


    /**
     * Retrieves the N last records stored
     * @see #isStorageSupported()
     * @param maxRecords
     * @param clear if true, also clears records from driver or sensor memory
     * @return a list of records sorted by acquisition time (empty if no records are available)
     * @throws SensorException if storage is not supported or a problem occured while reading storage
     */
    public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException;


    /**
     * Retrieves all records stored by the driver or sensor
     * @see #isStorageSupported()
     * @param clear if true, also clears records from driver or sensor memory
     * @return the list of all records in storage sorted by acquisition time (empty if no records are available)
     * @throws SensorException if storage is not supported or a problem occured while reading storage
     */
    public List<DataBlock> getAllRecords(boolean clear) throws SensorException;


    /**
     * Clears all records currently stored in driver or sensor memory
     * @see #isStorageSupported()
     * @return number of records removed
     * @throws SensorException if storage is not supported or a problem occured while clearing storage
     */
    public int clearAllRecords() throws SensorException;

}
