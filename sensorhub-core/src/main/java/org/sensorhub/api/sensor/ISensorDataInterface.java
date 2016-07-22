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
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.data.IStreamingDataInterface;


/**
 * <p>
 * Interface to be implemented by all sensor drivers connected to the system.
 * <p>Data provided by this interface can be actual measurements but also status
 * information. Each sensor output is mapped to a separate instance of this
 * interface, allowing them to have completely independent sampling rates.</p>
 * <p>Even when storage is not supported, implementations MUST at least be
 * capable of retaining the latest record received from sensor and make it
 * available with {@link #getLatestRecord()}.</p>
 * <p>Implementation MUST send events of type 
 * {@link org.sensorhub.api.sensor.SensorDataEvent} when new data is produced
 * by sensor.
 * </p>
 * 
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public interface ISensorDataInterface extends IStreamingDataInterface
{

    /**
     * Allows by-reference access to parent sensor module
     */
    @Override
    public ISensorModule<?> getParentModule();
    
    
    /**
     * Gets this output interface name.
     * <p><i>It MUST be the name reported in the map by getXXXOutputs methods
     * of {@link org.sensorhub.api.sensor.ISensorModule}</i></p>
     * @see org.sensorhub.api.sensor.ISensorModule#getAllOutputs()
     * @return name of this output interface
     */
    @Override
    public String getName();


    /**
     * Checks data storage capability
     * <p><i>Note that storage can be provided by the sensor itself or by the
     * driver.</i></p>
     * @return true if record storage is supported internally, false otherwise
     */
    public boolean isStorageSupported();


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
     */
    @Override
    public DataBlock getLatestRecord();


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
