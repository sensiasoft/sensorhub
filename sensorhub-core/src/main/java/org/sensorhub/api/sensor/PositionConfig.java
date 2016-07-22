/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.sensor;

import org.sensorhub.api.config.DisplayInfo;


/**
 * <p>
 * Simple configuration class for setting static location and orientation
 * </p>
 *
 * @author Alex Robin
 * @since Apr 2, 2016
 */
public class PositionConfig
{
    
    public static class LLALocation
    {
        @DisplayInfo(label="Latitude", desc="Geodetic latitude, in degrees")
        public double lat;
        
        @DisplayInfo(label="Longitude", desc="Longitude, in degrees")
        public double lon;
        
        @DisplayInfo(label="Altitude", desc="Height above ellipsoid, in meters")
        public double alt;
    }
    
    
    public static class CartesianLocation
    {
        @DisplayInfo(desc="X coordinate, in meters")
        public double x;
        
        @DisplayInfo(desc="Y coordinate, in meters")
        public double y;
        
        @DisplayInfo(desc="Z coordinate, in meters")
        public double z;
    }
    
    
    public static class EulerOrientation
    {
        @DisplayInfo(desc="Heading (or yaw) angle about Z axis in degrees")
        public double heading;
        
        @DisplayInfo(desc="Pitch angle about Y axis, in degrees")
        public double pitch;
        
        @DisplayInfo(desc="Roll angle about X axis, in degrees")
        public double roll;
    }
    
    
    @DisplayInfo(desc="Location in EPSG:4979 coordinate reference frame")
    public LLALocation location;
    
    
    @DisplayInfo(desc="Orientation as Euler angles in NED coordinate reference frame.\nOrder of rotations is z-y’-x\" (in rotating frame) or x-y-z (in fixed frame)")
    public EulerOrientation orientation;
    
    
}
