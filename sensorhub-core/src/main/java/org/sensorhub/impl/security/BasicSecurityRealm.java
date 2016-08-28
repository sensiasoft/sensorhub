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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.security.IAuthorizer;
import org.sensorhub.api.security.IPermission;
import org.sensorhub.api.security.IPermissionPath;
import org.sensorhub.api.security.IRoleRegistry;
import org.sensorhub.api.security.IUserInfo;
import org.sensorhub.api.security.IUserRegistry;
import org.sensorhub.api.security.IUserRole;
import org.sensorhub.impl.module.AbstractModule;


public class BasicSecurityRealm extends AbstractModule<BasicSecurityRealmConfig> implements IUserRegistry, IRoleRegistry, IAuthorizer
{
    BasicSecurityRealmConfig config;
    Map<String, IUserInfo> users = new LinkedHashMap<String, IUserInfo>();
    Map<String, IUserRole> roles = new LinkedHashMap<String, IUserRole>();
    IAuthorizer authz = new DefaultAuthorizerImpl(this);
    

    public void init() throws SensorHubException
    {
        roles.put("admin", new IUserRole() {
            
            public String getId()
            {
                return "admin";
            }
            
            public String getName()
            {
                return "Administrator";
            }
            
            public String getDescription()
            {
                return "Administrators have all permissions";
            }

            public Collection<IPermissionPath> getAllowList()
            {
                ArrayList<IPermissionPath> allowList = new ArrayList<IPermissionPath>();
                allowList.add(new PermissionSetting(new WildcardPermission()));
                return allowList;
            }

            public Collection<IPermissionPath> getDenyList()
            {
                return Collections.EMPTY_LIST;
            }            
        });
        
        final IPermission sosRoot = new ModulePermissions("5cb05c9c-9e08-4fa1-8731-ff41e246bdc1");
        final IPermission sosRead = new ItemPermission(sosRoot, "read");
        roles.put("guest", new IUserRole() {
            
            public String getId()
            {
                return "guest";
            }
            
            public String getName()
            {
                return "Guest";
            }
            
            public String getDescription()
            {
                return "Guests can only view certain things";
            }

            public Collection<IPermissionPath> getAllowList()
            {
                ArrayList<IPermissionPath> allowList = new ArrayList<IPermissionPath>();
                allowList.add(new PermissionSetting(sosRead));
                //allowList.add(new PermissionSetting(new ItemPermission(sosRead, "caps")));
                //allowList.add(new PermissionSetting(new ItemPermission(sosRead, "obs")));
                return allowList;
            }

            public Collection<IPermissionPath> getDenyList()
            {
                ArrayList<IPermissionPath> denyList = new ArrayList<IPermissionPath>();
                denyList.add(new PermissionSetting(new ItemPermission(sosRead, "caps")));
                //denyList.add(new PermissionSetting(new ItemPermission(sosRead, "sensor")));
                return denyList;
            }            
        });
        
        users.put("admin", new IUserInfo() {
            
            public String getId()
            {
                return "admin";
            }

            public String getName()
            {
                return "Administrator";
            }
            
            public String getPassword()
            {
                return "pwd";
            }

            public Collection<String> getRoles()
            {
                return Arrays.asList("admin");
            }

            public Collection<IPermissionPath> getAllowList()
            {
                return Collections.EMPTY_LIST;
            }

            public Collection<IPermissionPath> getDenyList()
            {
                return Collections.EMPTY_LIST;
            }

            public Map<String, Object> getAttributes()
            {
                return Collections.EMPTY_MAP;
            }
        });

        users.put("alex", new IUserInfo() {
            
            public String getId()
            {
                return "alex";
            }

            public String getName()
            {
                return "Alex Robin";
            }
            
            public String getPassword()
            {
                return "pwd";
            }

            public Collection<String> getRoles()
            {
                return Arrays.asList("guest"/*, "admin"*/);
            }

            public Collection<IPermissionPath> getAllowList()
            {
                return Collections.EMPTY_LIST;
            }

            public Collection<IPermissionPath> getDenyList()
            {
                return Collections.EMPTY_LIST;
            }

            public Map<String, Object> getAttributes()
            {
                return Collections.EMPTY_MAP;
            }
        });
        
        
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        // TODO Auto-generated method stub
        
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        // TODO Auto-generated method stub
        
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public IUserInfo getUserInfo(String userID)
    {
        return users.get(userID);
    }
    
    
    @Override
    public IUserRole getRoleInfo(String roleID)
    {
        return roles.get(roleID);
    }


    @Override
    public boolean isAuthorized(IUserInfo user, IPermissionPath request)
    {
        return authz.isAuthorized(user, request);
    }
    
}
