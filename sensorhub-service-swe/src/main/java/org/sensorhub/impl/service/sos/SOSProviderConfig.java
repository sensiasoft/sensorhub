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