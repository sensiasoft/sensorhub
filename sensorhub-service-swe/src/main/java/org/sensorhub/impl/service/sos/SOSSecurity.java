/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleSecurity;
import org.sensorhub.impl.security.ItemPermission;


public class SOSSecurity extends ModuleSecurity
{    
    public final IPermission sos_read;
    public final IPermission sos_read_caps;
    public final IPermission sos_read_sensor;
    public final IPermission sos_read_foi;
    public final IPermission sos_read_obs;
    public final IPermission sos_insert;
    public final IPermission sos_insert_sensor;
    public final IPermission sos_insert_obs;
    public final IPermission sos_update;
    public final IPermission sos_update_sensor;
    public final IPermission sos_update_obs;
    public final IPermission sos_delete;
    public final IPermission sos_delete_sensor;
    public final IPermission sos_delete_obs;
    
    
    public SOSSecurity(SOSService sos)
    {
        super(sos);
        
        // register permission structure
        sos_read = new ItemPermission(rootPerm, "read");
        sos_read_caps = new ItemPermission(sos_read, "caps");
        sos_read_sensor = new ItemPermission(sos_read, "sensor");
        sos_read_foi = new ItemPermission(sos_read, "foi");
        sos_read_obs = new ItemPermission(sos_read, "obs");
        
        sos_insert = new ItemPermission(rootPerm, "insert");
        sos_insert_sensor = new ItemPermission(sos_insert, "sensor");
        sos_insert_obs = new ItemPermission(sos_insert, "obs");
        
        sos_update = new ItemPermission(rootPerm, "update");
        sos_update_sensor = new ItemPermission(sos_update, "sensor");
        sos_update_obs = new ItemPermission(sos_update, "obs");
        
        sos_delete = new ItemPermission(rootPerm, "delete");
        sos_delete_sensor = new ItemPermission(sos_delete, "sensor");
        sos_delete_obs = new ItemPermission(sos_delete, "obs");
        
        // create permissions for each offering
        for (SOSProviderConfig offering: sos.getConfiguration().dataProviders)
        {
            String permName = getOfferingPermissionName(offering.uri);
            new ItemPermission(sos_read_caps, permName);
            new ItemPermission(sos_read_obs, permName);
            new ItemPermission(sos_insert_obs, permName);
            new ItemPermission(sos_update_obs, permName);
            new ItemPermission(sos_delete_obs, permName);
            new ItemPermission(sos_update_sensor, permName);
            new ItemPermission(sos_delete_sensor, permName);
        }
        
        SensorHub.getInstance().getSecurityManager().registerModulePermissions(sos.getLocalID(), rootPerm);
    }
    
    
    public void check(String offeringUri, IPermission perm) throws SecurityException
    {
        String permName = getOfferingPermissionName(offeringUri);
        check(perm.getChildren().get(permName));
    }
    
    
    protected String getOfferingPermissionName(String offeringUri)
    {
        return "offering[" + offeringUri + "]";
    }
}
