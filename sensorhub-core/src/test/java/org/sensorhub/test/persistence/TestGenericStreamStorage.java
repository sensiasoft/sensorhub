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
import org.sensorhub.impl.persistence.GenericStreamStorage;
import org.sensorhub.impl.persistence.InMemoryBasicStorage;
import org.sensorhub.impl.persistence.StreamStorageConfig;
import org.sensorhub.test.sensor.FakeSensor;
import org.sensorhub.test.sensor.FakeSensorData;


public class TestGenericStreamStorage
{
    private final static String OUTPUT_NAME = "out1";
    File configFile;
    FakeSensorData fakeSensorData;
    GenericStreamStorage storage;
    ModuleRegistry registry;
    
    
    @Before
    public void setup() throws Exception
    {
        configFile = new File("test-config.json");
        configFile.deleteOnExit();
        SensorHub hub = SensorHub.createInstance(new SensorHubConfig(configFile.getAbsolutePath(), configFile.getParent()));
        registry = hub.getModuleRegistry();
        
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = false;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        ISensorModule<?> sensor = (ISensorModule<?>)registry.loadModule(sensorCfg);
        fakeSensorData = new FakeSensorData((FakeSensor)sensor, OUTPUT_NAME, 10, 0.1, 10);
        ((FakeSensor)sensor).setDataInterfaces(fakeSensorData);
        
        // create test storage
        StreamStorageConfig genericStorageConfig = new StreamStorageConfig();
        genericStorageConfig.moduleClass = GenericStreamStorage.class.getCanonicalName();
        genericStorageConfig.name = "SensorStorageTest";
        genericStorageConfig.enabled = true;
        genericStorageConfig.dataSourceID = sensor.getLocalID();
        StorageConfig storageConfig = new StorageConfig();
        storageConfig.moduleClass = InMemoryBasicStorage.class.getCanonicalName();
        genericStorageConfig.storageConfig = storageConfig;
        storage = (GenericStreamStorage)registry.loadModule(genericStorageConfig);
    }
    
    
    @Test
    public void testAddRecordToStorage() throws Exception
    {
        while (fakeSensorData.isEnabled())
            Thread.sleep((long)(fakeSensorData.getAverageSamplingPeriod() * 500));
        
        Thread.sleep(100);
        assertEquals(fakeSensorData.getMaxSampleCount(), storage.getDataStores().get(OUTPUT_NAME).getNumRecords());
    }
    
    
    @After
    public void cleanup()
    {
        configFile.delete();
    }
}
