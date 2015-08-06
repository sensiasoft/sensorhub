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

import org.sensorhub.api.common.EntityEvent;
import org.sensorhub.api.data.DataEvent.Type;
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
public class DataEvent extends EntityEvent<Type>
{
	/**
     * Possible event types for a DataEvent
     */
    public static enum Type
    {
        /**
         * New data is available from a sensor or process
         */
        NEW_DATA_AVAILABLE,
        
        /**
         * For data that has already been sent but needs to be updated.<br/>
         * (e.g. a model has actualized some of its predicted data)
         */
        DATA_UPDATED
    };
	
	
	/**
	 * New data that triggered this event.<br/>
	 * Multiple records can be associated to a single event because with high
	 * rate producers, it is often not practical to generate an event for
	 * every single record of measurements.
	 */
	protected DataBlock[] records;
	
	
	/**
	 * Constructs a data event with no related entity
	 * @param timeStamp time of event generation (unix time in milliseconds, base 1970)
     * @param dataInterface stream interface that generated the associated data
	 * @param records arrays of records that triggered this notification
	 */
	public DataEvent(long timeStamp, IStreamingDataInterface dataInterface, DataBlock ... records)
	{
	    this(timeStamp, null, dataInterface, records);
	}
	
	
	/**
     * Constructs a data event associated to an identifiable entity
     * @param timeStamp time of event generation (unix time in milliseconds, base 1970)
	 * @param entityID Unique ID of entity that produced the data records
     * @param dataInterface stream interface that generated the associated data
     * @param records arrays of records that triggered this notification
     */
    public DataEvent(long timeStamp, String entityID, IStreamingDataInterface dataInterface, DataBlock ... records)
    {
        this.type = Type.NEW_DATA_AVAILABLE;
        this.timeStamp = timeStamp;
        //this.producerID = dataInterface.getParentModule().getLocalID();
        //this.channelID = dataInterface.getName();
        this.source = dataInterface;
        this.relatedEntityID = entityID;
        this.records = records;
    }
		
	
	@Override
    public Type getType()
    {
        return type;
    }
	
	
    @Override
    public IStreamingDataInterface getSource()
    {
        return (IStreamingDataInterface)this.source;
    }
	   
	
    /**
     * @return description of the data records attached to this event
     */
    public DataComponent getRecordDescription()
    {
        return ((IStreamingDataInterface)source).getRecordDescription();
    }


    /**
     * @return list of data records produced
     */
    public DataBlock[] getRecords()
    {
        return records;
    }
}
