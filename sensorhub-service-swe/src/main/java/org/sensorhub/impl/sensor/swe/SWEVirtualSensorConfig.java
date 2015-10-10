/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.swe;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;


/**
 * <p>
 * Configuration for a SWE Virtual Sensor.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2015
 */
public class SWEVirtualSensorConfig extends SensorConfig
{
    @DisplayInfo(label="Sensor UID", desc="Unique ID of sensor to connect to on SOS and SPS servers")
    public String sensorUID;
    
    @DisplayInfo(label="SOS Endpoint URL", desc="SOS endpoint URL to fetch data from")
    public String sosEndpointUrl;
    
    @DisplayInfo(label="Observed Properties", desc="List of observed properties URI to make available as outputs")
    public List<String> observedProperties = new ArrayList<String>();
    
    @DisplayInfo(label="Use WebSockets for SOS", desc="Set if WebSocket protocol should be used to get streaming data from SOS")
    public boolean sosUseWebsockets = false;
    
    @DisplayInfo(label="SPS Endpoint URL", desc="SPS endpoint URL to send commands to")
    public String spsEndpointUrl;
    
    //@DisplayInfo(label="Use WebSockets for SPS", desc="Set if websockets protocol should be used to send commands to SPS")
    //public boolean spsUseWebsockets = false;
           
    
    public SWEVirtualSensorConfig()
    {
        this.moduleClass = SWEVirtualSensor.class.getCanonicalName();
    }

}
