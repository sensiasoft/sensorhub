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

import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.EntityEvent;


/**
 * <p>
 * Special type of immutable event carrying status data by reference
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public class SensorControlEvent extends EntityEvent<CommandStatus.StatusCode>
{
	/**
	 * Status of the command that triggered this event
	 */
    protected CommandStatus status;
	
	
	/**
     * Constructs the event for an individual sensor
     * @param timeStamp unix time of event generation
     * @param controlInterface sensor control interface that generated the event
     * @param status status of command at time the event is generated
     */
    public SensorControlEvent(long timeStamp, ISensorControlInterface controlInterface, CommandStatus status)
    {
        this(timeStamp, controlInterface.getParentSensor().getLocalID(), controlInterface, status);
    }
    
    
    /**
     * Constructs the event for a sensor that is part of a network
     * @param timeStamp unix time of event generation
     * @param sensorID ID of individual sensor in the network
     * @param controlInterface sensor control interface that generated the event
     * @param status status of command at time the event is generated
     */
    public SensorControlEvent(long timeStamp, String sensorID, ISensorControlInterface controlInterface, CommandStatus status)
    {
        this.timeStamp = timeStamp;
        this.source = controlInterface;
        //this.producerID = controlInterface.getParentSensor().getLocalID();
        this.relatedEntityID = sensorID;
        this.type = status.status;
    }
    
    
    /**
     * Gets the unique ID of the sensor related to this event.<br/>
     * For sensor networks, it can be either the ID of the network as a whole 
     * (if the command was global) or the ID of one of the sensor in the
     * network (if the command was sent to a particular sensor).
     * @return the ID of the sensor that this event refers to
     */
    public String getSensorID()
    {
        return relatedEntityID;
    }
	

    public CommandStatus getStatus()
    {
        return status;
    }
    
    
    @Override
    public ISensorControlInterface getSource()
    {
        return (ISensorControlInterface)this.source;
    }
}
