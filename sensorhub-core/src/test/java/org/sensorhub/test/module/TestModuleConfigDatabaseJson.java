/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.service.ServiceConfig;
import org.sensorhub.impl.module.ModuleConfigDatabaseJson;
import static org.junit.Assert.*;


public class TestModuleConfigDatabaseJson
{
    ModuleConfigDatabaseJson configDb;
    File configFolder;
    
    
    @Before
    public void setup()
    {
        configFolder = new File("junittest/");
        configFolder.mkdirs();
        configDb = new ModuleConfigDatabaseJson(configFolder.getAbsolutePath());
    }
    
    
    @Test
    public void testAdd() throws Exception
    {
        SensorConfig config1 = new SensorConfig();
        config1.id = UUID.randomUUID().toString();
        config1.name = "Sensor1";
        config1.moduleClass = "org.sensorhub.sensor.***";
        config1.enableHistory = true;
        configDb.add(config1);
        
        displayFiles();
    }
    
    
    @Test(expected=RuntimeException.class)
    public void testAddAndRemove() throws Exception
    {
        SensorConfig config1 = new SensorConfig();
        config1.id = UUID.randomUUID().toString();
        config1.name = "Sensor1";
        config1.moduleClass = "org.sensorhub.sensor.***";
        config1.enableHistory = true;
        configDb.add(config1);
        
        displayFiles();
        
        configDb.remove(config1.id);
        configDb.get(config1.id);
    }
    
    
    @Test
    public void testAddAndReadBack() throws Exception
    {
        ModuleConfig storedConf;
        
        ProcessConfig config1 = new ProcessConfig();
        config1.id = UUID.randomUUID().toString();
        config1.name = "Process1";
        config1.moduleClass = "org.sensorhub.process.ProcessModel";
        configDb.add(config1);
        
        ServiceConfig config2 = new ServiceConfig();
        config2.id = UUID.randomUUID().toString();
        config2.name = "Service1";
        config2.moduleClass = "org.sensorhub.service.SosService";
        configDb.add(config2);
        
        ServiceConfig config3 = new ServiceConfig();
        config3.id = UUID.randomUUID().toString();
        config3.name = "Service2";
        config3.moduleClass = "org.sensorhub.service.SpsService";
        configDb.add(config3);
        
        StorageConfig config4 = new StorageConfig();
        config4.id = UUID.randomUUID().toString();
        config4.name = "DB1";
        config4.moduleClass = "org.sensorhub.persistence.FeatureStorage";
        configDb.add(config4);
        
        displayFiles();
        
        storedConf = configDb.get(config1.id);
        assertTrue(storedConf.id.equals(config1.id));
        assertTrue(storedConf.name.equals(config1.name));
        assertTrue(storedConf.moduleClass.equals(config1.moduleClass));
        
        storedConf = configDb.get(config2.id);
        assertTrue(storedConf.id.equals(config2.id));
        assertTrue(storedConf.name.equals(config2.name));
        assertTrue(storedConf.moduleClass.equals(config2.moduleClass));
        
        storedConf = configDb.get(config3.id);
        assertTrue(storedConf.id.equals(config3.id));
        assertTrue(storedConf.name.equals(config3.name));
        assertTrue(storedConf.moduleClass.equals(config3.moduleClass));
        
        storedConf = configDb.get(config4.id);
        assertTrue(storedConf.id.equals(config4.id));
        assertTrue(storedConf.name.equals(config4.name));
        assertTrue(storedConf.moduleClass.equals(config4.moduleClass));
    }
    
    
    private void displayFiles() throws Exception
    {
        for (File f: configFolder.listFiles())
        {
            // print out file
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line;
            while ((line = reader.readLine()) != null)
                System.out.println(line);
        }
    }
    
    
    @After
    public void cleanup()
    {
        for (File f: configFolder.listFiles())
            f.delete();
        configFolder.delete();
    }
}
