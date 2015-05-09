/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;


/**
 * <p>
 * Base class for all events that relates to a particular entity.<br/>
 * An entity in SensorHub can be a sensor, a process, etc. and this type of
 * event is useful when it is related to a particular entity within a group
 * such as a sensor network or a process grid.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <EventTypeEnum> Enum of possible event sub-type
 * @since Apr 23, 2015
 */
public abstract class EntityEvent<EventTypeEnum extends Enum<?>> extends Event<EventTypeEnum>
{

    /**
     * @see #getRelatedEntityID()
     */
    protected String relatedEntityID;
    
    
    /**
     * Gets the unique ID of the entity related to this event.<br/>
     * For group of entities (e.g. sensor networks), it will be either the ID
     * of the group as a whole (if the event is global) or the ID of a single
     * entity within the group (if the event applies only to that entity)
     * @return Unique ID of related entity
     */
    public String getRelatedEntityID()
    {
        return relatedEntityID;
    }
    
}
