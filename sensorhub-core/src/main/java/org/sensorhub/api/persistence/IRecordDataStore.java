/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;

import java.util.Iterator;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.IEventProducer;


/**
 * <p>
 * Base interface for all record storage implementations
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <KeyType> Type of record key
 * @param <FilterType> Type of record-filter object
 * @since Nov 6, 2010
 */
public interface IRecordDataStore<KeyType extends DataKey, FilterType extends IDataFilter> extends IEventProducer
{	
    
    /**
     * @return parent storage instance
     */
    public IStorageModule<?> getParentStorage();
    
    
    /**
     * @return number of records contained in this storage
     */
    public int getNumRecords();
    
    
	/**
	 * @return description of data blocks persisted in this storage
	 */
	public DataComponent getRecordDescription();
	
	
	/**
     * @return recommended encoding for records contained in this storage
     */
    public DataEncoding getRecommendedEncoding();
	
	
	/**
     * Retrieves raw data block with the specified key
     * @param key Record key
     * @return data block or null if no record with the specified key was found
     */
    public DataBlock getDataBlock(KeyType key);
    
    
	/**
	 * Retrieves record with the specified key
	 * @param key record key
	 * @return record or null if no record with the specified key was found
	 */
	public IDataRecord<KeyType> getRecord(KeyType key);
	
	
	/**
     * Gets iterator of raw data blocks matching the specified filter
     * @param filter filtering parameters
     * @return an iterator among data blocks matching the filter
     */
    public Iterator<DataBlock> getDataBlockIterator(FilterType filter);
    
    
	/**
	 * Gets iterator of records matching the specified filter
	 * @param filter filtering parameters
	 * @return an iterator among records matching the filter
	 */
	public Iterator<? extends IDataRecord<KeyType>> getRecordIterator(FilterType filter);
	
	
	/**
	 * Computes the (potentially approximate) number of records matching the given filter.
	 * Since the returned value can be approximate and the number of matching records can
	 * change before or even during the actual call to {@link #getRecordIterator(IDataFilter)},
	 * the exact number of records can only be obtained by counting the number of records
	 * returned by the iterator next() function.
	 * @param filter filtering parameters
	 * @return number of matching records
	 */
	public int getNumMatchingRecords(FilterType filter);
	
	
	/**
     * Persists data block in storage
     * @param key key object to associate to record
     * @param data actual record data
     * @return stored data key (containing auto-generated id and time stamp if non was provided)
     */
    public KeyType store(KeyType key, DataBlock data);
    
    
	/**
     * Updates record with specified key with new data
     * @param key key of record to update
     * @param data new data block to assign to the record
     */
    public void update(KeyType key, DataBlock data);
    
    
    /**
     * Removes record with the specified key
     * @param key record key
     */
    public void remove(KeyType key);
    
    
	/**
     * Removes all records matching the filter
     * @param filter filtering parameters
     * @return number of deleted records
     */
    public int remove(FilterType filter);
    
}
