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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Type of event generated when new data is available from storage.
 * It is immutable and carries data records by reference
 * </p>
 *
 * <p>Copyright (c) 2014</p>
 * @author Alexandre Robin
 * @since Dec 5, 2014
 */
public class StorageDataEvent extends StorageEvent
{
	private static final long serialVersionUID = 2124599187504793797L;
    
	
	/**
	 * Description of data records contained in this event (by reference) 
	 */
	protected DataComponent recordDescription;
	
	
	/**
	 * New data that triggered this event.
	 * Multiple records can be associated to a single event because for performance
	 * reasonsn with high rate sensors, it is often not practical to generate an
	 * event for every single record of measurements.
	 */
	protected DataBlock[] records;
	
	
	/**
	 * Constructor from list of records with their descriptor
	 * @param timeStamp unix time of event generation
     * @param dataStore data store that received the associated data
	 * @param records arrays of records that triggered this notification
	 */
	public StorageDataEvent(long timeStamp, IRecordDataStore<?,?> dataStore, DataBlock ... records)
	{
		super(timeStamp, dataStore.getParentStorage().getLocalID(), Type.STORE);
		this.source = dataStore;
		this.recordDescription = dataStore.getRecordDescription();
		this.records = records;
	}


    public DataComponent getRecordDescription()
    {
        return recordDescription;
    }


    public DataBlock[] getRecords()
    {
        return records;
    }


    @Override
    public IRecordDataStore<?,?> getSource()
    {
        return (IRecordDataStore<?,?>)this.source;
    }
}
