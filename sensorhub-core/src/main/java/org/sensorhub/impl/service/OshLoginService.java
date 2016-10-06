/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service;

import java.security.Principal;
import javax.security.auth.Subject;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;
import org.sensorhub.api.security.IUserInfo;
import org.sensorhub.api.security.IUserRegistry;


public class OshLoginService implements LoginService
{
    IdentityService identityService = new DefaultIdentityService();
    IUserRegistry users;
    //Map<String, UserIdentity>
    
    
    public static class RolePrincipal implements Principal
    {
        private final String _roleName;
        
        public RolePrincipal(String name)
        {
            _roleName=name;
        }
        
        public String getName()
        {
            return _roleName;
        }
    }
    
    
    public OshLoginService(IUserRegistry users)
    {
        this.users = users;
    }
    
    
    @Override
    public IdentityService getIdentityService()
    {
        return identityService;
    }


    @Override
    public String getName()
    {
        return "OpenSensorHub: Authentication Required";
    }


    @Override
    public UserIdentity login(String username, Object credential)
    {
        if (username == null)
            return null;
        
        boolean isCert = false;
        if (username.startsWith("CN="))
        {
            username = username.substring(3, username.indexOf(','));
            isCert = true;
        }
        
        IUserInfo user = users.getUserInfo(username);
        if (user == null)
            return null;
        
        UserIdentity identity = null;        
        if (!isCert)
        {
            Credential storedCredential = Credential.getCredential(user.getPassword());                
            if (storedCredential.check(credential))
                identity = createUserIdentity(user, credential);
        }
        else
            identity = createUserIdentity(user, credential);
        
        return identity;
    }
    
    
    protected UserIdentity createUserIdentity(final IUserInfo user, Object credential)
    {
        Principal principal = new Principal() {
            public String getName()
            {
                return user.getId();
            }
        };
        Subject subject = new Subject();
        subject.getPrincipals().add(principal);
        subject.getPrivateCredentials().add(credential);            
        subject.setReadOnly();
        
        String[] roles = user.getRoles().toArray(new String[0]);
        UserIdentity identity = identityService.newUserIdentity(subject, principal, roles);
        return identity;
    }


    @Override
    public void logout(UserIdentity user)
    {        
    }


    @Override
    public void setIdentityService(IdentityService identityService)
    {
        this.identityService = identityService;
    }


    @Override
    public boolean validate(UserIdentity user)
    {
        return true;
    }

}
