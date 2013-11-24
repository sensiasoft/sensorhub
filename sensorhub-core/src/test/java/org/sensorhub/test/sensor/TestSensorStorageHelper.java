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
 
 Please Contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.sensor;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorStorageConfig;
import org.sensorhub.impl.module.ModuleConfigDatabaseJson;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.SensorStorageHelper;


public class TestSensorStorageHelper
{
    File configFolder;
    ModuleConfigDatabaseJson configDb;
    FakeSensorData fakeSensorData;
    InMemoryStorage db;
    
    
    @Before
    public void setup() throws Exception
    {
        configFolder = new File("junit-test/");
        configFolder.mkdirs();
        configDb = new ModuleConfigDatabaseJson(configFolder.getAbsolutePath());        
        ModuleRegistry registry = ModuleRegistry.create(configDb, true);
        registry.loadAllModules();
        
        // create test storage
        StorageConfig storageConfig = new StorageConfig();
        storageConfig.moduleClass = InMemoryStorage.class.getCanonicalName();
        storageConfig.name = "SensorStorageTest";
        storageConfig.storagePath = new File(configFolder, "sensordb.dbs").getAbsolutePath();
        db = (InMemoryStorage)registry.loadModule(storageConfig);
               
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = true;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        IModule<?> sensor = registry.loadModule(sensorCfg);
        fakeSensorData = new FakeSensorData("out1", true, 10, 0.1, 10);
        ((FakeSensor)sensor).setDataInterfaces(fakeSensorData);
    }
    
    
    @Test
    public void testCreate() throws Exception
    {
        SensorStorageConfig config = new SensorStorageConfig();
        config.moduleClass = SensorStorageHelper.class.getCanonicalName();
        config.name = "Sensor Storage Helper";
        config.storageID = ModuleRegistry.getInstance().getLoadedModules().get(0).getLocalID();
        config.sensorID = ModuleRegistry.getInstance().getLoadedModules().get(1).getLocalID();
        ModuleRegistry.getInstance().loadModule(config);
        
        Thread.sleep((long)((fakeSensorData.maxSampleCount+1) * 1000.0 / fakeSensorData.getAverageSamplingRate()));
        
        assertEquals(fakeSensorData.maxSampleCount, db.recordList.size());
    }
    
    
    @After
    public void cleanup()
    {
        try
        {
            FileUtils.deleteDirectory(configFolder);
        }
        catch (IOException e)
        {
        }
    }
}
