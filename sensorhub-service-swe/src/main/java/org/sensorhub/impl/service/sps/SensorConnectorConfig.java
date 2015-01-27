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

package org.sensorhub.impl.service.sps;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.common.SensorHubException;


/**
 * <p>
 * Configuration class for SPS connectors using the sensor API
 * </p>
 *
 * <p>Copyright (c) 2014</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Dec 13, 2014
 */
public class SensorConnectorConfig extends SPSConnectorConfig
{

    /**
     * Local ID of sensor to send commands to
     */
    public String sensorID;
    
    
    /**
     * Names of sensor command interfaces to hide from SPS
     */
    public List<String> hiddenCommands = new ArrayList<String>();


    @Override
    protected ISPSConnector getConnector() throws SensorHubException
    {
        return new DirectSensorConnector(this);
    }
}
