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

import net.opengis.sensorml.v20.AbstractProcess;


/**
 * <p>
 * Base interface for all modules that provide a SensorML description
 * (such as sensors, actuators and processes)
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 21, 2015
 */
public interface IModuleWithDescription
{
        
    /**
     * @return The object's globally unique identifier
     */
    public String getUniqueIdentifier();
    
    
    /**
     * Retrieves most current SensorML description of the entity whose data
     * is provided by this module. All implementations must return an instance
     * of AbstractProcess with a valid unique identifier.<br/>
     * In the case of a module generating data from multiple entities (e.g. 
     * sensor network), this returns the description of the group as a whole.
     * Descriptions of individual entities within the group are retrived using
     * {@link IMultiSourceDataProducer#getCurrentDescription(String)}
     * @return AbstractProcess SensorML description of the data producer or
     * null if none is available at the time of the call 
     */
    public AbstractProcess getCurrentDescription();


    /**
     * Used to check when SensorML description was last updated.
     * This is useful to avoid requesting the object when it hasn't changed.
     * @return Date/time of last description update as unix time (ms since 1970) or
     * {@link Long#MIN_VALUE} if description was never updated.
     */
    public long getLastDescriptionUpdate();

}