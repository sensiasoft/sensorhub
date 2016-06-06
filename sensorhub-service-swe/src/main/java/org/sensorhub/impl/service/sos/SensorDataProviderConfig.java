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
import org.sensorhub.api.config.DisplayInfo;


/**
 * <p>
 * Configuration class for SOS data providers using the sensor API.
 * A storage can also be associated to the sensor so that archive request
 * for this sensor data can be handled through the same offering.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class SensorDataProviderConfig extends StreamDataProviderConfig
{

    @DisplayInfo(desc="Local ID of sensor module to use as data source for live-stream requests")
    public String sensorID;
    
    
    @Override
    protected ISOSDataProviderFactory getFactory(SOSServlet service) throws SensorHubException
    {
        if (storageID != null)
            return new SensorWithStorageProviderFactory(service, this);
        else
            return new SensorDataProviderFactory(service, this);
    }

}
