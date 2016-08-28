/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.security;

import org.sensorhub.api.config.DisplayInfo;


/**
 * <p>
 * Basic security config group to be used within module configurations
 * </p>
 *
 * @author Alex Robin
 * @since Apr 2, 2016
 */
public class SecurityConfig
{
    
    @DisplayInfo(label="Module alias", desc="Module alias that can be used in permission specs instead of module ID")
    public String alias;
    
    
    @DisplayInfo(label="Require Authentication", desc="Set to require users to be authentified before they can use this module")
    public boolean requireAuth = false;
    
    
    @DisplayInfo(desc="Enables fine-grained permission-based access control for this module")
    public boolean enableAccessControl = false;
    
}
