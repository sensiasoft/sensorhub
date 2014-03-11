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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorStorageConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.SensorHubConfig;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.persistence.InMemoryBasicStorage;
import org.sensorhub.impl.sensor.SensorStorageHelper;


public class TestSensorStorageHelper
{
    File configFile;
    FakeSensorData fakeSensorData;
    InMemoryBasicStorage db;
    ModuleRegistry registry;
    
    
    @Before
    public void setup() throws Exception
    {
        configFile = new File("juni-test-config.json");
        configFile.deleteOnExit();
        SensorHub hub = SensorHub.createInstance(new SensorHubConfig(configFile.getAbsolutePath(), configFile.getParent()));
        registry = hub.getModuleRegistry();
        
        // create test storage
        StorageConfig storageConfig = new StorageConfig();
        storageConfig.enabled = true;
        storageConfig.moduleClass = InMemoryBasicStorage.class.getCanonicalName();
        storageConfig.name = "SensorStorageTest";
        storageConfig.storagePath = new File(configFile.getParent(), "sensordb.dbs").getAbsolutePath();
        db = (InMemoryBasicStorage)registry.loadModule(storageConfig);
               
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = true;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        IModule<?> sensor = registry.loadModule(sensorCfg);
        fakeSensorData = new FakeSensorData((FakeSensor)sensor, "out1", true, 10, 0.1, 10);
        ((FakeSensor)sensor).setDataInterfaces(fakeSensorData);
    }
    
    
    @Test
    public void testCreate() throws Exception
    {
        SensorStorageConfig config = new SensorStorageConfig();
        config.enabled = true;
        config.moduleClass = SensorStorageHelper.class.getCanonicalName();
        config.name = "Sensor Storage Helper";
        config.storageID = registry.getLoadedModules().get(0).getLocalID();
        config.sensorID = registry.getLoadedModules().get(1).getLocalID();
        registry.loadModule(config);
        
        Thread.sleep((long)((fakeSensorData.maxSampleCount+1) * 1000.0 / fakeSensorData.getAverageSamplingPeriod()));
        
        assertEquals(fakeSensorData.maxSampleCount, db.getNumRecords());
    }
    
    
    @After
    public void cleanup()
    {
        configFile.delete();
    }
}
