/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.client.sost;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.service.ClientConfig;


/**
 * <p>
 * Configuration class for the SOS-T client module
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 6, 2015
 */
public class SOSTClientConfig extends ClientConfig
{
        
    @DisplayInfo(desc="SOS endpoint URL where the requests are sent")
    public String sosEndpointUrl;

    
    @DisplayInfo(desc="Local ID of sensor to register with SOS")
    public String sensorID;

    
    @DisplayInfo(desc="Set to true to use a persistent Insertresult connection")
    public boolean usePersistentConnection;

    
    @DisplayInfo(desc="Maximum number of connection errors before we stop sending data to remote server")
    public int maxConnectErrors = 10;
    
    
    public SOSTClientConfig()
    {
        this.moduleClass = SOSTClient.class.getCanonicalName();
    }
}
