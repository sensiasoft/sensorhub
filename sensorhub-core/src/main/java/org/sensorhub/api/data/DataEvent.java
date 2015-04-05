/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.data;

import org.sensorhub.api.common.Event;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Type of event generated when new data is available from a data producer.
 * It is immutable and carries data by reference.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 20, 2015
 */
public class DataEvent extends Event
{
	/**
     * Possible event types for a DataEvent
     */
    public enum Type
    {
        NEW_DATA_AVAILABLE
    };
    
    
    /**
     * Type of data event
     */
    protected Type type;
    
    
	/**
	 * Description of data records contained in this event (by reference) 
	 */
	protected DataComponent recordDescription;
	
	
	/**
	 * New data that triggered this event.
	 * Multiple records can be associated to a single event because with high
	 * rate producers, it is often not practical to generate an event for
	 * every single record of measurements.
	 */
	protected DataBlock[] records;
	
	
	/**
	 * Constructor from list of records with their descriptor
	 * @param timeStamp time of event generation (unix time in milliseconds, base 1970)
     * @param source stream interface that generated the associated data
	 * @param records arrays of records that triggered this notification
	 */
	public DataEvent(long timeStamp, IStreamingDataInterface source, DataBlock ... records)
	{
	    this.type = Type.NEW_DATA_AVAILABLE;
	    this.timeStamp = timeStamp;
		this.source = source;
		this.recordDescription = source.getRecordDescription();
		this.records = records;
	}
	
	
	@Override
    public IStreamingDataInterface getSource()
    {
        return (IStreamingDataInterface)this.source;
    }
	
	
    public Type getType()
    {
        return type;
    }


    public DataComponent getRecordDescription()
    {
        return recordDescription;
    }


    public DataBlock[] getRecords()
    {
        return records;
    }
}
