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


public abstract class SOSProviderConfig
{

    /**
     * Flag set if provider is enabled, unset if disabled
     */
    public boolean enabled;
    
    
    /**
     * Provider/Offering URI
     * If null, it will be auto-generated from server URL and source metadata
     */
    public String uri;
    
    
    /**
     * Provider name
     * If null, it will be auto-generated from name of data source
     */
    public String name;
    
    
    /**
     * Provider description
     * It null, it will be auto-generated from source description
     */
    public String description;
    
    
    /**
     * Retrieves the factory associated with this type of data provider
     * @return
     */
    protected abstract IDataProviderFactory getFactory() throws SensorHubException;
}