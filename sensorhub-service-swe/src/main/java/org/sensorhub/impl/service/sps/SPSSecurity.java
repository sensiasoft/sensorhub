/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sps;

import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleSecurity;
import org.sensorhub.impl.security.ItemPermission;
import org.sensorhub.impl.security.ModulePermissions;


public class SPSSecurity extends ModuleSecurity
{    
    private final static String LABEL_CAPS = "Capabilities";
    private final static String LABEL_SENSOR = "Sensor Descriptions";
    private final static String LABEL_PARAMS = "Task Parameters";
    private final static String LABEL_TASK = "Task Status";
    
    public final IPermission sps_read;
    public final IPermission sps_read_caps;
    public final IPermission sps_read_sensor;
    public final IPermission sps_read_params;
    public final IPermission sps_read_task;
    public final IPermission sps_task;
    public final IPermission sps_task_submit;
    public final IPermission sps_task_feasibility;
    public final IPermission sps_task_update;
    public final IPermission sps_task_cancel;
    public final IPermission sps_task_reserve;
    
    
    public SPSSecurity(SPSService sps, boolean enable)
    {
        super(sps, "sps", enable);
        
        // register permission structure
        sps_read = new ItemPermission(rootPerm, "get");
        sps_read_caps = new ItemPermission(sps_read, "caps", LABEL_CAPS);
        sps_read_sensor = new ItemPermission(sps_read, "sensor", LABEL_SENSOR);
        sps_read_params = new ItemPermission(sps_read, "params", LABEL_PARAMS);
        sps_read_task = new ItemPermission(sps_read, "task", LABEL_TASK);
        
        sps_task = new ItemPermission(rootPerm, "tasking");
        sps_task_submit = new ItemPermission(sps_task, "submit");
        sps_task_feasibility = new ItemPermission(sps_task, "feasibility");
        sps_task_update = new ItemPermission(sps_task, "update");
        sps_task_cancel = new ItemPermission(sps_task, "cancel");
        sps_task_reserve = new ItemPermission(sps_task, "reserve");
        
        // register wildcard permission tree usable for all SPS services
        // do it at this point so we don't include specific offering permissions
        ModulePermissions templatePerm = rootPerm.cloneAsTemplatePermission("SPS Services");
        SensorHub.getInstance().getSecurityManager().registerModulePermissions(templatePerm);
                
        // create permissions for each offering
        for (SPSConnectorConfig offering: sps.getConfiguration().connectors)
        {
            String permName = getOfferingPermissionName(offering.uri);
            new ItemPermission(sps_read_sensor, permName);
            new ItemPermission(sps_read_params, permName);
            new ItemPermission(sps_read_task, permName);
            new ItemPermission(sps_task_submit, permName);
            new ItemPermission(sps_task_feasibility, permName);
            new ItemPermission(sps_task_reserve, permName);
            new ItemPermission(sps_task_update, permName);
        }
        
        // register this instance permission tree
        SensorHub.getInstance().getSecurityManager().registerModulePermissions(rootPerm);
    }
    
    
    public void checkPermission(String offeringUri, IPermission perm) throws SecurityException
    {
        String permName = getOfferingPermissionName(offeringUri);
        checkPermission(perm.getChildren().get(permName));
    }
    
    
    protected String getOfferingPermissionName(String offeringUri)
    {
        return "offering[" + offeringUri + "]";
    }
}
