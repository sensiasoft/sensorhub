/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.module;

import java.security.AccessControlException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.security.IPermission;
import org.sensorhub.api.security.IUserInfo;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.security.ItemPermission;
import org.sensorhub.impl.security.ModulePermissions;
import org.sensorhub.impl.security.PermissionRequest;


public class ModuleSecurity
{    
    protected final ModulePermissions rootPerm;
    public final IPermission module_init;
    public final IPermission module_start;
    public final IPermission module_stop;
    public final IPermission module_update;
    ThreadLocal<IUserInfo> currentUser = new ThreadLocal<IUserInfo>();
    
    
    public ModuleSecurity(IModule<?> module)
    {
        rootPerm = new ModulePermissions(module.getLocalID(), module.getClass());
        
        // register basic module permissions        
        module_init = new ItemPermission(rootPerm, "init", "Unallowed to initialize module");
        module_start = new ItemPermission(rootPerm, "start", "Unallowed to start module");
        module_stop = new ItemPermission(rootPerm, "stop", "Unallowed to stop module");
        module_update = new ItemPermission(rootPerm, "update", "Unallowed to update module configuration");
        
        SensorHub.getInstance().getSecurityManager().registerModulePermissions(module.getLocalID(), rootPerm);
    }
    
    
    /**
     * Checks if the current user has the given permission
     * @param perm
     * @return true if user is permitted, false otherwise
     */
    public boolean hasPermission(IPermission perm)
    {
        // retrieve currently logged in user
        IUserInfo user = currentUser.get();
        if (user == null)
            //throw new SecurityException(perm.getErrorMessage() + ": No user specified");
            return true;
        
        // request authorization
        return SensorHub.getInstance().getSecurityManager().isAuthorized(user, new PermissionRequest(perm));
    }
    
    
    /**
     * Checks if the current user has the given permission and throws
     * exception if it doesn't
     * @param perm
     * @throws SecurityException
     */
    public void check(IPermission perm) throws SecurityException
    {
        // retrieve currently logged in user
        IUserInfo user = currentUser.get();
        if (user == null)
            //throw new SecurityException(perm.getErrorMessage() + ": No user specified");
            return;
        
        // request authorization
        if (!hasPermission(perm))
            throw new AccessControlException(perm.getErrorMessage() + ": User=" + user.getId());
    }
    
    
    /**
     * Sets the user attempting to use this module in the current thread
     * @param userID
     */
    public void setCurrentUser(String userID)
    {
        // lookup user info 
        IUserInfo user = SensorHub.getInstance().getSecurityManager().getUserInfo(userID);
        if (user == null)
            throw new SecurityException("Permission denied: Unknown user " + userID);
        
        currentUser.set(user);
    }
    
    
    /**
     * Clears the user associated to the current thread
     */
    public void clearCurrentUser()
    {
        currentUser.remove();
    }
}
