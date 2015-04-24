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

import org.sensorhub.api.common.EntityEvent;
import org.sensorhub.api.sensor.SensorEvent.Type;


/**
 * <p>
 * Event generated when sensor state/configuration changes.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public class SensorEvent extends EntityEvent<Type>
{
    /**
	 * Possible event types for a SensorEvent
	 */
    public static enum Type
	{
		/**
		 * Sensor connected to SensorHub
		 */
        CONNECTED,
        
        /**
         * Sensor disconnected from SensorHub
         */
		DISCONNECTED,
		
		/**
		 * Sensor added to network
		 */
		SENSOR_ADDED,
		
		/**
		 * Sensor removed from network (not just disconnected)
		 */
		SENSOR_REMOVED,
		
		/**
		 * Sensor configuration changed (should be reflected by SensorML)
		 */
		SENSOR_CHANGED
	};
	
	
	/**
	 * Constructs the event for an individual sensor
	 * @param timeStamp unix time of event generation
	 * @param source sensor module that generated the event
	 * @param type type of event
	 */
	public SensorEvent(long timeStamp, ISensorModule<?> source, Type type)
	{
	    this(timeStamp, source.getLocalID(), source, type);
	}
	
	
	/**
	 * Constructs the event for a sensor that is part of a network
	 * @param timeStamp unix time of event generation
	 * @param sensorID Id of individual sensor in the network
	 * @param source sensor module that generated the event
	 * @param type type of event
	 */
	public SensorEvent(long timeStamp, String sensorID, ISensorModule<?> source, Type type)
    {
        this.timeStamp = timeStamp;
        this.source = source;
        this.sourceModuleID = source.getLocalID();
        this.relatedObjectID = sensorID;
        this.type = type;
    }
	

	/**
     * For individual sensors, this method will return the same value as
     * {@link #getSourceModuleID()}, but for sensor networks, this can be
     * either the ID of the network as a whole (if the change is global) or
     * the ID of one of the sensor within the network (if the change applies
     * only to that particular sensor, e.g. recalibration).
     * @return the ID of the sensor that this event refers to
     */
    public String getSensorID()
    {
        return relatedObjectID;
    }
    
    
	@Override
    public ISensorModule<?> getSource()
    {
        return (ISensorModule<?>)this.source;
    }
}
