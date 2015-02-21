/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.ogc;

import org.sensorhub.api.config.Annotations.DisplayInfo;
import org.sensorhub.api.service.ServiceConfig;
import org.vast.util.ResponsibleParty;


/**
 * <p>
 * Abstract configuration class for all OGC service types
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 6, 2013
 */
@SuppressWarnings("serial")
public abstract class OGCServiceConfig extends ServiceConfig
{
    public class CapabilitiesInfo
    {
        public String title;
        public String description;
        public String[] keywords;
        public String fees;
        public String accessConstraints;
        public ResponsibleParty serviceProvider = new ResponsibleParty(); 
    }

    
    /**
     * Information to include in the service capabilities document
     */
    public CapabilitiesInfo ogcCapabilitiesInfo = new CapabilitiesInfo();
    
    
    /**
     * Enables/disables HTTP GET bindings on operations that support it
     */
    @DisplayInfo(label="Enable HTTP GET")
    public boolean enableHttpGET = true;
    
    
    /**
     * Enables/disables HTTP POST bindings on operations that support it
     */
    @DisplayInfo(label="Enable HTTP POST")
    public boolean enableHttpPOST = true;
    
    
    /**
     * Enables/disables HTTP SOAP bindings on operations that support it
     */
    @DisplayInfo(label="Enable HTTP SOAP")
    public boolean enableSOAP = true;
}
