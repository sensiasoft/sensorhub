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

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.security.IAuthorizer;
import org.sensorhub.api.security.IPermission;
import org.sensorhub.api.security.IPermissionPath;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.api.security.IUserInfo;
import org.sensorhub.api.security.IUserRegistry;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.util.Asserts;


public class SecurityManagerImpl implements ISecurityManager
{
    private static final Logger log = LoggerFactory.getLogger(SecurityManagerImpl.class);
    
    ModuleRegistry moduleRegistry;
    Map<String, IPermission> modulePermissions = new LinkedHashMap<String, IPermission>();
    WeakReference<IUserRegistry> users;
    WeakReference<IAuthorizer> authz;
    
    
    public SecurityManagerImpl(ModuleRegistry moduleRegistry)
    {
        this.moduleRegistry = moduleRegistry;
    }
    
    
    @Override
    public boolean isAccessControlEnabled()
    {        
        return (users != null && users.get() != null &&
                authz != null && authz.get() != null);
    }
    
    
    @Override
    public boolean ensureAccessControlEnabled()
    {        
        if (users == null || users.get() == null)
        {
            // wait for all osh modules to be loaded by registry
            moduleRegistry.waitForAllModulesLoaded();
            
            // wait for user registry to start
            for (IModule<?> module: moduleRegistry.getLoadedModules())
            {
                if (module instanceof IUserRegistry)
                {
                    if (module.waitForState(ModuleState.STARTED, 10000))
                    {
                        log.info("User registry provided by module " + MsgUtils.moduleString(module));
                        users = new WeakReference<IUserRegistry>((IUserRegistry)module);
                        break;
                    }
                }
            }
        }
         
        if (authz == null || authz.get() == null)
        {
            // wait for authorizer to start
            for (IModule<?> module: moduleRegistry.getLoadedModules())
            {
                if (module instanceof IAuthorizer)
                {
                    if (module.waitForState(ModuleState.STARTED, 10000))
                    {
                        log.info("Authorization realm provided by module " + MsgUtils.moduleString(module));
                        authz = new WeakReference<IAuthorizer>((IAuthorizer)module);
                        break;
                    }
                }
            }
        }
        
        return isAccessControlEnabled();
    }
        
    
    @Override
    public IUserInfo getUserInfo(String userID)
    {
        Asserts.checkNotNull(users, "No IUserRegistry implementation registered");
        
        IUserRegistry users = this.users.get();
        if (users != null)
            return users.getUserInfo(userID);
        else
            return null;
    }
    
    
    @Override
    public boolean isAuthorized(IUserInfo user, IPermissionPath request)
    {
        Asserts.checkNotNull(authz, "No IAuthorizer implementation registered");
        
        IAuthorizer authz = this.authz.get();
        if (authz != null)
            return authz.isAuthorized(user, request);
        else
            return true;
    }


    @Override
    public void registerModulePermissions(IPermission perm)
    {
        modulePermissions.put(perm.getName(), perm);
    }


    @Override
    public IPermission getModulePermissions(String moduleIdString)
    {
        if (IPermission.WILDCARD.equals(moduleIdString))
            return new WildcardPermission("All Modules");
        
        return modulePermissions.get(moduleIdString);
    }
    
    
    @Override
    public Collection<IPermission> getAllModulePermissions()
    {
        return Collections.unmodifiableCollection(modulePermissions.values());
    }

}
