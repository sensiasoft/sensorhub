/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.processing;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.config.DisplayInfo;


/**
 * <p>
 * Config class for specifying a data source for a processing module
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 26, 2015
 */
public abstract class DataSourceConfig
{
    public static final String AUTO_CREATE = "AUTO_CREATE";
    
    
    /*
     * for wiring data source outputs to process inputs at the component level
     */
    public static class InputLinkConfig
    {        
        @DisplayInfo(label="Source", desc="'/' separated path of source component in data source outputs (starting with name of output/datastore)")
        public String source;
        
        
        @DisplayInfo(label="Destination", desc="'/' separated path of destination component in process inputs (starting with name of process input)")
        public String destination;
    }
    
    
    @DisplayInfo(label="Input Connections", desc="Specification of connections between source outputs and process inputs")
    public List<InputLinkConfig> inputConnections = new ArrayList<InputLinkConfig>();
    
}
