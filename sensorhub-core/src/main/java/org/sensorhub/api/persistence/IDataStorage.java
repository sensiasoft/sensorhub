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

package org.sensorhub.api.persistence;

import java.util.Iterator;
import java.util.List;
import org.sensorhub.api.common.IEventProducer;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;


/**
 * <p>
 * Base interface for all data storage implementations
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 6, 2010
 */
public interface IDataStorage<KeyType extends DataKey, FilterType extends IDataFilter, ConfigType extends StorageConfig> extends IStorageModule<ConfigType>, IEventProducer
{	
    /**
     * Returns number of records contained in this storage
     * @return
     */
    public int getNumRecords();
    
    
	/**
	 * Retrieves description of data blocks persisted in this storage
	 * @return
	 */
	public DataComponent getRecordDescription();
	
	
	/**
     * Retrieves raw data block with the specified id
     * @param id
     * @return data block or null if no record with the specified id were found
     */
    public DataBlock getDataBlock(long id);
    
    
	/**
	 * Retrieves record with the specified id
	 * @param id
	 * @return record or null if no record with the specified id were found
	 */
	public IDataRecord<KeyType> getRecord(long id);
	
	
	/**
     * Retrieves raw data blocks matching filtering criteria from storage
     * @param filter
     * @return a list of data blocks matching the filter
     */
    public List<DataBlock> getDataBlocks(FilterType filter);
    
    
	/**
	 * Retrieves record matching filtering criteria from storage
	 * @param filter
	 * @return a list of records matching the filter
	 */
	public List<IDataRecord<KeyType>> getRecords(FilterType filter);
	
	
	/**
     * Gets iterator of raw data blocks matching the specified filter
     * @param filter
     * @return an iterator among data blocks matching the filter
     */
    public Iterator<DataBlock> getDataBlockIterator(FilterType filter);
    
    
	/**
	 * Gets iterator of records matching the specified filter
	 * @param filter
	 * @return an iterator among records matching the filter
	 */
	public Iterator<IDataRecord<KeyType>> getRecordIterator(FilterType filter);
	
	
	/**
     * Persists data block in storage
     * @paran key 
     * @param data
     * @return stored data key (containing auto-generated id and time stamp if non was provided)
     */
    public KeyType store(KeyType key, DataBlock data);
    
    
	/**
     * Updates record with specified id with new key and data
     * @param id id of record to update
     * @paran key new key for upated record
     * @param data new data block to assign to the record
     * @return updated data key (containing auto-generated id and time stamp if non was provided)
     */
    public DataKey update(long id, KeyType key, DataBlock data);
    
    
	/**
     * Removes all records matching the filter
     * @param filter
     * @return number of deleted records
     */
    public int remove(FilterType filter);
	
    
    /**
     * Removes record with the specified id
     * @param id
     */
    public abstract void remove(long id);
    
}
