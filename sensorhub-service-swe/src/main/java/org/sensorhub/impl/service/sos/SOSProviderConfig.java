/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;


public abstract class SOSProviderConfig
{

    @DisplayInfo(desc="Set if provider is enabled, unset if disabled")
    public boolean enabled;
    
    
    @Required
    @DisplayInfo(desc="Offering URI as exposed in capabilities")
    public String uri;
    
    
    @DisplayInfo(desc="Provider name (if null, it will be set to the name of the data source)")
    public String name;
    
    
    @DisplayInfo(desc="Provider description (if null, it will be auto-generated from the data source name)")
    public String description;
    
    
    @DisplayInfo(desc="Maximum number of FoI IDs listed in capabilities")
    public int maxFois = 10;
    
    
    /**
     * Retrieves the factory associated with this type of data provider
     * @param service Parent SOS service
     * @return
     */
    protected abstract ISOSDataProviderFactory getFactory(SOSServlet service) throws SensorHubException;
}