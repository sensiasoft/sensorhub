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

package org.sensorhub.api.persistence;

import java.util.Iterator;
import java.util.List;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.IEventProducer;


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
