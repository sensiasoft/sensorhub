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

package org.sensorhub.impl.service.sps;


public abstract class SPSConnectorConfig
{

    /**
     * Flag set if connector is enabled, unset if disabled
     */
    public boolean enabled;
    
    
    /**
     * Offering URI
     * If null, it will be auto-generated from server URL and target metadata
     */
    public String uri;
    
    
    /**
     * Offering name
     * If null, it will be auto-generated from name of target
     */
    public String name;
    
    
    /**
     * Offering description
     * It null, it will be auto-generated from name of target
     */
    public String description;

}