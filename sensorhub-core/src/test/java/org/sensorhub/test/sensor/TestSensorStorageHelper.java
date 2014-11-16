/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
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
        sensorCfg.enabled = false;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        IModule<?> sensor = registry.loadModule(sensorCfg);
        fakeSensorData = new FakeSensorData((FakeSensor)sensor, "out1", true, 10, 0.1, 10);
        ((FakeSensor)sensor).setDataInterfaces(fakeSensorData);
    }
    
    
    @Test
    public void testAddRecordToStorage() throws Exception
    {
        SensorStorageConfig config = new SensorStorageConfig();
        config.enabled = true;
        config.moduleClass = SensorStorageHelper.class.getCanonicalName();
        config.name = "Sensor Storage Helper";
        config.storageID = registry.getLoadedModules().get(0).getLocalID();
        config.sensorID = registry.getLoadedModules().get(1).getLocalID();
        registry.loadModule(config);
        registry.enableModule(config.sensorID);
        
        Thread.sleep((long)((fakeSensorData.maxSampleCount * fakeSensorData.getAverageSamplingPeriod() + 1.) * 1000));
        
        assertEquals(fakeSensorData.maxSampleCount, db.getNumRecords());
    }
    
    
    @After
    public void cleanup()
    {
        configFile.delete();
    }
}
