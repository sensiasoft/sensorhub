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
import org.sensorhub.api.config.DisplayInfo.Required;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.sensor.PositionConfig;
import org.sensorhub.api.sensor.PositionConfig.CartesianLocation;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.SensorConfig;


/**
 * <p>
 * Configuration class for SensorGroup modules
 * </p>
 *
 * @author Alex Robin
 * @since Apr 1, 2016
 */
public class SensorSystemConfig extends SensorConfig
{    
   
    public static class SensorMember
    {
        public String name;
        public SensorConfig config;
        public CartesianLocation location;
        public EulerOrientation orientation;
    }
    
    
    public static class ProcessMember
    {
        public String name;
        public ProcessConfig config;
    }
    
        
    @Required
    @DisplayInfo(desc="Unique ID (full URN or only suffix) to use for the sensor system or 'auto' to use the UUID randomly generated the first time the module is initialized")
    public String uniqueID;
    
    
    @DisplayInfo(label="Fixed Position", desc="Fixed system position on earth")
    public PositionConfig position;
    
    
    @DisplayInfo(label="System Sensors", desc="Configuration of sensor components of this sensor system")
    public List<SensorMember> sensors = new ArrayList<SensorMember>();    
    
    
    @DisplayInfo(label="System Processes", desc="Configuration of processing components of this sensor system")
    public List<ProcessMember> processes = new ArrayList<ProcessMember>();


    @Override
    public LLALocation getLocation()
    {
        if (position == null)
            return null;
        return position.location;
    }


    @Override
    public EulerOrientation getOrientation()
    {
        if (position == null)
            return null;
        return position.orientation;
    }
}
