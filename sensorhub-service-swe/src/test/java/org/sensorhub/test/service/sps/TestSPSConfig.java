/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.UUID;
import org.junit.Test;
import org.sensorhub.impl.module.ModuleConfigDatabaseJson;
import org.sensorhub.impl.service.sps.SPSServiceConfig;
import org.sensorhub.impl.service.sps.SPSService;


public class TestSPSConfig
{
    
    @Test
    public void testAddToJsonDatabase() throws Exception
    {
        File configFolder = new File("junittest/");
        configFolder.mkdirs();        
        ModuleConfigDatabaseJson db = new ModuleConfigDatabaseJson(configFolder.getAbsolutePath());
        
        SPSServiceConfig config = new SPSServiceConfig();
        config.id = UUID.randomUUID().toString();
        config.name = "SPS Service #1";
        config.moduleClass = SPSService.class.getCanonicalName();
        config.enabled = true;
        config.enableSOAP = false;
        config.ogcCapabilitiesInfo.title = "SensorHub SPS Service";
        config.ogcCapabilitiesInfo.description = "An SPS service deployed on SensorHub that allows to control sensors";
        config.ogcCapabilitiesInfo.keywords = new String[] {"OGC", "sensors", "control"};
        config.ogcCapabilitiesInfo.serviceProvider.setCity("Toulouse");
        db.add(config);
        
        // display stored file
        for (File f: configFolder.listFiles())
        {
            // print out file
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;
            while ((line = reader.readLine()) != null)
                System.out.println(line);
                        
            // delete file
            f.delete();
        }
        
        configFolder.delete();
    }
}
