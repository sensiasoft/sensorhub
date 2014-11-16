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

package org.sensorhub.api.sensor;

import java.util.List;
import org.sensorhub.api.module.ModuleConfig;


/**
 * <p>
 * Configuration options for sensors/actuators
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 5, 2010
 */
public class SensorConfig extends ModuleConfig
{
    private static final long serialVersionUID = 2834895717702955136L;


    /**
     * URL of SensorML description of the sensor
     */
    public String sensorML;
    
    
    /**
     * Automatically activate sensor when plugged in
     */
    public boolean autoActivate = true;
    
    
    /**
     * Enables/disables maintenance of SensorML history
     */
    public boolean enableHistory = true;
    
    
    /**
     * Allows hiding some of the sensor interfaces
     */
    public String[] hiddenIO;
    
        
    /**
     * Driver configuration groups (potentially one for each layer of a protocol stack)
     * Can be included in SensorML v2.0
     */
    public List<SensorDriverConfig> driverConfigs;
}
