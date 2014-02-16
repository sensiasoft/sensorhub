/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
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
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
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
