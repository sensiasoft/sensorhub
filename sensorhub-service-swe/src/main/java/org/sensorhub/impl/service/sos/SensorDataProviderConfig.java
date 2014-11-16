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

import org.sensorhub.api.common.SensorHubException;


/**
 * <p>
 * Configuration class for SOS data providers using the sensor API
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class SensorDataProviderConfig extends SOSProviderConfig
{
    
    /**
     * Local ID of sensor to use as data source
     */
    public String sensorID;
    
    
    /**
     * Names of sensor outputs to hide from SOS
     */
    public String[] hiddenOutputs = new String[0];
    
    
    /**
     * If true, forward "new sensor data" events via the WS-Notification
     * interface of the service
     */
    public boolean activateNotifications;


    @Override
    protected IDataProviderFactory getFactory() throws SensorHubException
    {
        return new SensorDataProviderFactory(this);
    }

}
