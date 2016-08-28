/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.security;

import org.sensorhub.api.security.IAuthorizer;
import org.sensorhub.api.security.IPermissionPath;
import org.sensorhub.api.security.IRoleRegistry;
import org.sensorhub.api.security.IUserInfo;
import org.sensorhub.api.security.IUserPermissions;


public class DefaultAuthorizerImpl implements IAuthorizer
{
    IRoleRegistry roles;
    
    
    public DefaultAuthorizerImpl(IRoleRegistry roleRegistry)
    {
        this.roles = roleRegistry;
    }
    
    
    @Override
    public boolean isAuthorized(IUserInfo user, IPermissionPath requestedPerm)
    {
        // check all roles
        for (String roleName: user.getRoles())
        {
            IUserPermissions role = roles.getRoleInfo(roleName);
            
            // stop here if this role allows access
            if (hasPermission(role, requestedPerm))
                return true;
        }
        
        // check user own permissions
        return hasPermission((IUserPermissions)user, requestedPerm);
    }
    
    
    private boolean hasPermission(IUserPermissions permissions, IPermissionPath requestedPerm)
    {
        boolean match = false;
        
        // check allowed permissions
        for (IPermissionPath perm: permissions.getAllowList())
        {
            if (perm.implies(requestedPerm))
            {
                match = true;
                break;
            }
        }
        
        // check denied permissions
        for (IPermissionPath perm: permissions.getDenyList())
        {
            if (perm.implies(requestedPerm))
            {
                match = false;
                break;
            }
        }
        
        return match;
    }
}
