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

import java.util.LinkedHashMap;
import java.util.Map;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.security.IAuthorizer;
import org.sensorhub.api.security.IPermissionPath;
import org.sensorhub.api.security.IRoleRegistry;
import org.sensorhub.api.security.IUserInfo;
import org.sensorhub.api.security.IUserRegistry;
import org.sensorhub.api.security.IUserRole;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.security.BasicSecurityRealmConfig.RoleConfig;
import org.sensorhub.impl.security.BasicSecurityRealmConfig.UserConfig;


public class BasicSecurityRealm extends AbstractModule<BasicSecurityRealmConfig> implements IUserRegistry, IRoleRegistry, IAuthorizer
{
    Map<String, IUserInfo> users = new LinkedHashMap<String, IUserInfo>();
    Map<String, IUserRole> roles = new LinkedHashMap<String, IUserRole>();
    IAuthorizer authz = new DefaultAuthorizerImpl(this);
    

    public void init() throws SensorHubException
    {
        // build user map
        users.clear();
        for (UserConfig user: config.users)
            users.put(user.userID, user);
        
        // build role map
        roles.clear();
        for (RoleConfig role: config.roles)
        {
            role.refreshPermissionLists();
            roles.put(role.roleID, role);
        }
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
