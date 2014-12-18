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
 * Interface to be implemented by all sensor drivers connected to the system.
 * <p>Data provided by this interface can be actual measurements but also status
 * information. Each sensor output is mapped to a separate instance of this
 * interface, allowing them to have completely independent sampling rates.</p>
 * <p>Implementations must at least be capable of retaining the latest record
 * received from sensor and make it available with {@link #getLatestRecord()}</p>
 * <p>If push is supported, implementation MUST produce events of type
 * {@link org.sensorhub.api.sensor.SensorDataEvent} when new data is received
 * or polled from sensor.
 * </p>
 * 
 * <p>Copyright (c) 2014</p>
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
     * Gets the interface name.
     * <p><i>It should be the name reported in the map by getXXXOutputs methods
     * of {@link org.sensorhub.api.sensor.ISensorModule}</i></p>
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
     * <p><i>Note that storage can be provided by the sensor itself or by the
     * driver.</i></p>
     * @return true if record storage is supported internally, false otherwise
     */
    public boolean isStorageSupported();


    /**
     * Checks push capability
     * <p><i>Note that 'push' can generally be simulated by the driver even if
     * the sensor doesn't support it natively.</i></p>
     * @return true if notifications are sent when new data is available, false otherwise
     */
    public boolean isPushSupported();


    /**
     * Gets the average rate at which this interface produces data
     * @return sampling period in seconds
     */
    public double getAverageSamplingPeriod(); // used to know how often to poll


    /**
     * Retrieves the record definition for this output
     * <p><i>Note that this is usually sent by reference and MUST not be modified
     * by the caller. If you really need to modify it, first get an independent
     * copy using {@link net.opengis.swe.v20.DataComponent#copy()}</i></p>
     * @return a DataComponent object defining the structure of the output
     */
    public DataComponent getRecordDescription();


    /**
     * Provides the recommended encoding for this sensor data
     * <p><i>Note that this is usually sent by reference and MUST not be modified
     * by the caller. If you really need to modify it, first get an independent
     * copy using {@link net.opengis.swe.v20.DataEncoding#copy()}</i></p>
     * @return recommended encoding description
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
    //  SHOULD BE IMPLEMENTED AS A COMMAND

    /**
     * Gets the latest record received on this data channel. Even implementations with no
     * storage support must always retain the last record and implement this method.
     * <p>When storage is supported, this method does not cause data to be removed from memory.</p>
     * <p><i>Note that this does not trigger a new measurement but simply retrieves the
     * result of the last measurement made.</i></p>
     * @return the last measurement record or null if no data is available
     * @throws SensorException if there is a problem retrieving latest measurement from sensor
     */
    public DataBlock getLatestRecord() throws SensorException;
    
    
    /**
     * Used to check when the last measurement was made.
     * <p><i>Note: this method is useful to know if a measurement has really been made since the
     * last call to {@link #getLatestRecord}.</i></p>
     * @return time of last measurement as julian time (1970) or NaN if no measurement was made yet
     */
    public double getLatestRecordTime();


    /**
     * Gets the internal storage capacity.<br/>
     * This method must be implemented if {@link #isStorageSupported()} returns true.
     * @return maximum number of records that can be stored
     * @throws SensorException if storage is not supported or a problem occured while
     *         checking sensor on-board storage capacity
     */
    public int getStorageCapacity() throws SensorException;


    /**
     * Retrieves number of record currectly available from driver or on-board sensor memory.<br/>
     * This method must be implemented if {@link #isStorageSupported()} returns true.
     * @return the number of available records
     * @throws SensorException if storage is not supported or a problem occured while
     *         reading from on-board sensor storage
     */
    public int getNumberOfAvailableRecords() throws SensorException;


    /**
     * Retrieves the N last records stored.<br/>
     * This method must be implemented if {@link #isStorageSupported()} returns true.
     * @param maxRecords Maximum number of records to retrieve
     * @param clear if true, also clears records from driver or sensor memory
     * @return a list of records sorted by acquisition time (empty if no records are available)
     * @throws SensorException if storage is not supported or a problem occured while 
     *         reading from on-board sensor storage
     */
    public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException;


    /**
     * Retrieves all records stored by the driver or sensor.<br/>
     * This method must be implemented if {@link #isStorageSupported()} returns true.
     * @param clear if true, also clears records from driver or sensor memory
     * @return the list of all records in storage sorted by acquisition time (empty if no records are available)
     * @throws SensorException if storage is not supported or a problem occured 
     *         while reading from on-board sensor storage
     */
    public List<DataBlock> getAllRecords(boolean clear) throws SensorException;


    /**
     * Clears all records currently stored in driver or sensor memory.<br/>
     * This method must be implemented if {@link #isStorageSupported()} returns true.
     * @return number of records removed
     * @throws SensorException if storage is not supported or a problem occured while
     *         clearing on-board sensor storage
     */
    public int clearAllRecords() throws SensorException;

}
