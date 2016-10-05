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

import org.sensorhub.api.security.IPermission;


public class PermissionFactory
{
    
    
    public static PermissionSetting newPermissionSetting(String permString)
    {
        PermissionSetting permSetting = new PermissionSetting();
        
        String[] parts = permString.split("/");
        for (int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            
            IPermission perm;
            if (i == 0 && !IPermission.WILDCARD.equals(part))
                perm = new ModulePermissions(part);
            else
                perm = parsePermission(part);
            
            permSetting.add(perm);
        }
        
        // remove trailing wildcards
        while (permSetting.size() > 1 && permSetting.getLast() instanceof WildcardPermission)
            permSetting.removeLast();
        
        return permSetting;
    }
    
    
    public static IPermission parsePermission(String partString)
    {
        if (IPermission.WILDCARD.equals(partString))
            return new WildcardPermission();        
        else
            return new ItemPermission(null, partString);
    }
}
