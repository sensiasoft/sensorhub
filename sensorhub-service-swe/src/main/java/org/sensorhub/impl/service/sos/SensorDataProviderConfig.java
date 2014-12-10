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

package org.sensorhub.impl.service.sos;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.common.SensorHubException;


/**
 * <p>
 * Configuration class for SOS data providers using the sensor API.
 * A storage can also be associated to the sensor so that archive request
 * for this sensor data can be handled through the same offering.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class SensorDataProviderConfig extends SOSProviderConfig
{
    
    /**
     * Local ID of sensor to use as data source for
     * live-stream requests
     */
    public String sensorID;
    
    
    /**
     * Local ID of storage containing the sensor data
     * to use as data source for archive requests
     */
    public String storageID;
    
    
    /**
     * Names of sensor outputs to hide from SOS
     */
    public List<String> hiddenOutputs = new ArrayList<String>();
    
    
    /**
     * If true, forward "new sensor data" events via the WS-Notification
     * interface of the service
     */
    public boolean activateNotifications;


    @Override
    protected IDataProviderFactory getFactory() throws SensorHubException
    {
        if (storageID != null)
            return new SensorWithStorageProviderFactory(this);
        else
            return new SensorDataProviderFactory(this);
    }

}
