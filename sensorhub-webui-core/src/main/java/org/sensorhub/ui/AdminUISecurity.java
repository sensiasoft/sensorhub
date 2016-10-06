/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleSecurity;
import org.sensorhub.impl.security.ItemPermission;


public class AdminUISecurity extends ModuleSecurity
{    
    public final IPermission admin_access;
    public final IPermission admin_view;
    public final IPermission osh_shutdown;
    public final IPermission osh_restart;
    public final IPermission osh_saveconfig;
    public final IPermission module_init;
    public final IPermission module_start;
    public final IPermission module_stop;
    public final IPermission module_update;
    public final IPermission module_restart;
    public final IPermission module_add;
    public final IPermission module_remove;    
    
    
    public AdminUISecurity(AdminUIModule adminUI, boolean enable)
    {
        super(adminUI, "webadmin", enable);
        
        // register permission structure
        admin_access = rootPerm;
        admin_view = new ItemPermission(rootPerm, "view", "View Admin Interface"); 
        
        osh_shutdown = new ItemPermission(rootPerm, "shutdown", "Shutdown OSH"); 
        osh_restart = new ItemPermission(rootPerm, "restart", "Restart OSH"); 
        osh_saveconfig = new ItemPermission(rootPerm, "save_config", "Save OSH Configuration");

        module_add = new ItemPermission(rootPerm, "add", "Add Module");
        module_remove = new ItemPermission(rootPerm, "remove", "Remove Module");    
        module_init = new ItemPermission(rootPerm, "init", "Initialize Module");
        module_start = new ItemPermission(rootPerm, "start", "Start Module");
        module_stop = new ItemPermission(rootPerm, "stop", "Stop Module");
        module_update = new ItemPermission(rootPerm, "update_config", "Update Module Configuration");    
        module_restart = new ItemPermission(rootPerm, "restart", "Restart Module");            
        
        SensorHub.getInstance().getSecurityManager().registerModulePermissions(rootPerm);
    }
}
