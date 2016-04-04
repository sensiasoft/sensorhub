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
    public static class Location
    {
        public double lat;
        public double lon;
        public double alt;
    }
    
    
    public static class Orientation
    {
        public double pitch;
        public double roll;
        public double heading;
    }
    
    
    @DisplayInfo(desc="Location in EPSG:4979 coordinate reference frame")
    public Location location;
    
    
    @DisplayInfo(desc="Orientation in ENU coordinate reference frame")
    public Orientation orientation;
    
    
}
