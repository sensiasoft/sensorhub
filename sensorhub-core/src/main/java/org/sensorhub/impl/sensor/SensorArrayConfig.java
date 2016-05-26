/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.sensor.SensorConfig;


/**
 * <p>
 * Configuration class for SensorArray modules
 * </p>
 *
 * @author Alex Robin
 * @since Apr 1, 2016
 */
public class SensorArrayConfig extends SensorConfig
{
    @DisplayInfo(label="Common Configuration", desc="Common configuration for sensors in the array")
    public SensorConfig commonConfig;
    
        
    @DisplayInfo(label="Sensors Configuration", desc="Individual configuration of sensors in the array (will override common configuration)")
    public List<SensorConfig> sensors = new ArrayList<SensorConfig>();
}
