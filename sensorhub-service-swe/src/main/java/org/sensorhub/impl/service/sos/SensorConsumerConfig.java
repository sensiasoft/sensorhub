/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import org.sensorhub.api.common.SensorHubException;
import org.vast.ows.sos.ISOSDataConsumer;


/**
 * <p>
 * Configuration class for SOS data consumers using the sensor API.
 * A storage can also be associated so that it can be properly configured
 * when receiving InsertResultTemplate requests.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class SensorConsumerConfig extends SOSConsumerConfig
{
    
    /**
     * Local ID of sensor to use as data sink
     */
    public String sensorID;
    
    
    /**
     * Local ID of storage containing the sensor data
     * to use as data source for archive requests
     */
    public String storageID;


    @Override
    protected ISOSDataConsumer getConsumerInstance() throws SensorHubException
    {
        if (storageID != null)
            return new SensorWithStorageConsumer(this);
        else
            return new SensorDataConsumer(this);
    }

}
