/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.security;

import java.util.Collection;


/**
 * <p>
 * Management interface for SensorHub security
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jul 15, 2016
 */
public interface ISecurityManager extends IUserRegistry, IAuthorizer
{
    public final static String ANONYMOUS_USER = "anonymous";
    
    
    public boolean isAccessControlEnabled();
    
    
    public void registerModulePermissions(IPermission perm);
    
    
    public IPermission getModulePermissions(String moduleIdString);
    
    
    public Collection<IPermission> getAllModulePermissions();
}
