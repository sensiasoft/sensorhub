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
import org.sensorhub.api.common.SensorHubException;


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
     * Retrieves most current SensorML description of this module
     * @return AbstractProcess object containing all metadata about the module
     * @throws SensorHubException 
     */
    public abstract AbstractProcess getCurrentDescription() throws SensorHubException;


    /**
     * Used to check when SensorML description was last updated.
     * This is useful to avoid requesting the object when it hasn't changed.
     * @return date/time of last description update as julian time (1970)
     */
    public abstract double getLastDescriptionUpdate();

}