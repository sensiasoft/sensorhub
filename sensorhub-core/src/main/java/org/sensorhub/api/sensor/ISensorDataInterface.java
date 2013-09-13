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

import java.util.List;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.IEventProducer;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;


/**
 * <p>
 * Interface to be implemented by all sensor drivers connected to the system
 * Data from each sensor output is made available through this interface.
 * Data can be observations or status information.
 * Implementations must at least be capable of retaining the latest record
 * received from sensor until the getLatestRecord is called.
 * </p>
 * 
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 5, 2010
 */
public interface ISensorDataInterface extends IEventProducer
{
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
	public double getAverageSamplingRate(); // to know how often to poll
	
	
	/**
	 * Retrieves the record definition for this output
	 * @return a DataComponent object defining the structure of the output
     * @throws SensorException
	 */
	public DataComponent getRecordDescription() throws SensorException;
	
	
	/**
	 * Gets the latest record from this data channel. Data is also removed from input buffer.
	 * Implementations must at least retain the last record until it is retrieved or cleared explicitely
	 * @return the record as a DataBlock object
     * @throws SensorException
	 */
	public DataBlock getLatestRecord() throws SensorException;
	
		
	/**
     * Gets storage capacity
     * @see #isStorageSupported()
     * @return maximum number of records that can be stored or 0 if storage is not supported
     * @throws SensorException
     */
    public int getStorageCapacity() throws SensorException;
    
    
	/**
	 * Retrieves number of record currectly available from driver or sensor memory.
	 * Implementations with no storage support can only return 0 or 1
	 * @see #isStorageSupported()
	 * @return the number of available records
     * @throws SensorException
	 */
	public int getNumberOfAvailableRecords() throws SensorException;
	
	
	/**
	 * Retrieves the N last records stored
	 * @see #isStorageSupported()
	 * @param maxRecords
	 * @param clear if true, also clears records from driver or sensor memory
	 * @return a list of records (empty if no records are available)
     * @throws SensorException
	 */
	public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException;
	
	
	/**
	 * Retrieves all records stored by the driver or sensor
	 * @see #isStorageSupported()
	 * @param clear if true, also clears records from driver or sensor memory
	 * @return the list of all records in storage (empty if no records are available)
     * @throws SensorException
	 */
	public List<DataBlock> getAllRecords(boolean clear) throws SensorException;
	
	
	/**
	 * Clears all records currently stored in driver or sensor memory
	 * @see #isStorageSupported()
	 * @return number of records removed
     * @throws SensorException
	 */
	public int clearAllRecords() throws SensorException;
	
	
	/**
	 * Registers a listener to receive events when new data is available
	 * @see #isPushSupported()	
	 * @param listener
	 */
	@Override
	public void registerListener(IEventListener listener);

}
