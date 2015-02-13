/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.persistence;

import static org.junit.Assert.*;
import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.SensorHubConfig;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.persistence.InMemoryBasicStorage;
import org.sensorhub.impl.persistence.SensorStorageHelperConfig;
import org.sensorhub.impl.persistence.SensorStorageHelper;
import org.sensorhub.impl.persistence.StorageHelper;
import org.sensorhub.test.sensor.FakeSensor;
import org.sensorhub.test.sensor.FakeSensorData;


public class TestSensorStorageHelper
{
    File configFile;
    FakeSensorData fakeSensorData;
    InMemoryBasicStorage storage;
    ModuleRegistry registry;
    
    
    @Before
    public void setup() throws Exception
    {
        configFile = new File("juni-test-config.json");
        configFile.deleteOnExit();
        SensorHub hub = SensorHub.createInstance(new SensorHubConfig(configFile.getAbsolutePath(), configFile.getParent()));
        registry = hub.getModuleRegistry();
        
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = false;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        ISensorModule<?> sensor = (ISensorModule<?>)registry.loadModule(sensorCfg);
        fakeSensorData = new FakeSensorData((FakeSensor)sensor, "out1", true, 10, 0.1, 10);
        ((FakeSensor)sensor).setDataInterfaces(fakeSensorData);
        
        // create test storage
        StorageConfig storageConfig = new StorageConfig();
        storageConfig.enabled = true;
        storageConfig.moduleClass = InMemoryBasicStorage.class.getCanonicalName();
        storageConfig.name = "SensorStorageTest";
        storageConfig.storagePath = new File(configFile.getParent(), "sensordb.dbs").getAbsolutePath();
        storage = (InMemoryBasicStorage)registry.loadModule(storageConfig);
        StorageHelper.configureStorageForSensor(sensor, storage, false);
    }
    
    
    @Test
    public void testAddRecordToStorage() throws Exception
    {
        SensorStorageHelperConfig config = new SensorStorageHelperConfig();
        config.enabled = true;
        config.moduleClass = SensorStorageHelper.class.getCanonicalName();
        config.name = "Sensor Storage Helper";
        config.sensorID = registry.getLoadedModules().get(0).getLocalID();
        config.storageID = registry.getLoadedModules().get(1).getLocalID();
        registry.loadModule(config);
        registry.enableModule(config.sensorID);
        
        while (fakeSensorData.isEnabled())
            Thread.sleep((long)(fakeSensorData.getAverageSamplingPeriod() * 500));
        
        assertEquals(fakeSensorData.getMaxSampleCount(), storage.getDataStores().get("out1").getNumRecords());
    }
    
    
    @After
    public void cleanup()
    {
        configFile.delete();
    }
}
