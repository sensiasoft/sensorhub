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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.IdField;
import org.sensorhub.api.security.IPermissionPath;
import org.sensorhub.api.security.IUserInfo;
import org.sensorhub.api.security.IUserRole;
import org.sensorhub.api.security.SecurityModuleConfig;
import com.google.gson.annotations.SerializedName;


public class BasicSecurityRealmConfig extends SecurityModuleConfig
{
    @DisplayInfo(desc="List of users allowed access to this system")
    public List<UserConfig> users = new ArrayList<UserConfig>();
    
    @DisplayInfo(desc="List of security roles")
    public List<RoleConfig> roles = new ArrayList<RoleConfig>();
    
    
    @IdField("userID")
    public static class UserConfig implements IUserInfo
    {
        @SerializedName("id")
        @DisplayInfo(label="User ID")
        public String userID;
        public String name;
        public String password;
        //public String certificate;
        public List<String> roles = new ArrayList<String>();
        
        public String getId()
        {
            return userID;
        }
        
        public String getName()
        {
            return name;
        }
        
        public String getPassword()
        {
            return password;
        }
        
        public Collection<String> getRoles()
        {
            return roles;
        }
        
        public Map<String, Object> getAttributes()
        {
            return Collections.EMPTY_MAP;
        }
        
        public Collection<IPermissionPath> getAllowList()
        {
            return Collections.EMPTY_LIST;
        }
        
        public Collection<IPermissionPath> getDenyList()
        {
            return Collections.EMPTY_LIST;
        }
    }
    
    
    @IdField("roleID")
    public static class RoleConfig implements IUserRole
    {
        @SerializedName("id")
        @DisplayInfo(label="Role ID")
        public String roleID;
        public String name;
        public String description;
        public List<String> allow = new ArrayList<String>();
        public List<String> deny = new ArrayList<String>();
        
        transient Collection<IPermissionPath> allowList = new ArrayList<IPermissionPath>();
        transient Collection<IPermissionPath> denyList = new ArrayList<IPermissionPath>();

        public String getId()
        {
            return roleID;
        }
        
        public String getName()
        {
            return name;
        }        

        public String getDescription()
        {
            return description;
        }
        
        public Collection<IPermissionPath> getAllowList()
        {
            return allowList;
        }
        
        public Collection<IPermissionPath> getDenyList()
        {
            return denyList;
        }
        
        public void refreshPermissionLists()
        {
            allowList.clear();         
            for (String path: allow)
                allowList.add(PermissionFactory.newPermissionSetting(path));
            
            denyList.clear();         
            for (String path: deny)
                denyList.add(PermissionFactory.newPermissionSetting(path));
        }
    }
    
    
    public BasicSecurityRealmConfig()
    {
        this.moduleClass = BasicSecurityRealm.class.getCanonicalName();
    }
}
