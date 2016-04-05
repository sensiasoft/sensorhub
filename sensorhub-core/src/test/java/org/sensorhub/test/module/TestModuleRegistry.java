/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.Arrays;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.module.ModuleConfigJsonFile;
import org.sensorhub.impl.module.ModuleRegistry;


public class TestModuleRegistry
{
    File configFile;
    ModuleConfigJsonFile configDb;
    ModuleRegistry registry;
    
    
    @Before
    public void setup()
    {
        configFile = new File("test-conf.json");
        configFile.deleteOnExit();
        configDb = new ModuleConfigJsonFile(configFile.getAbsolutePath());        
        registry = new ModuleRegistry(configDb);
        registry.loadAllModules();
    }
    
    
    @Test
    public void testCloneConfig() throws Exception
    {
        ProcessConfig config1 = new ProcessConfig();
        config1.id = UUID.randomUUID().toString();
        config1.name = "Process1";
        config1.moduleClass = "org.sensorhub.process.ProcessModel";
        
        ProcessConfig clone1 = (ProcessConfig)config1.clone();
        assertTrue(clone1.id.equals(config1.id));
        assertTrue(clone1.name.equals(config1.name));
        assertTrue(clone1.moduleClass.equals(config1.moduleClass));
                
        SensorConfig config2 = new SensorConfig();
        config2.id = UUID.randomUUID().toString();
        config2.name = "sensor1";
        config2.moduleClass = "org.sensorhub.sensor.SensorDriver";
        config2.autoStart = true;
        config2.hiddenIO = new String[] {"input1", "input3"};
        
        SensorConfig clone2 = (SensorConfig)config2.clone();
        assertTrue(clone2.id.equals(config2.id));
        assertTrue(clone2.name.equals(config2.name));
        assertTrue(clone2.moduleClass.equals(config2.moduleClass));
        assertTrue(clone2.autoStart = config2.autoStart);
        assertTrue(Arrays.deepEquals(clone2.hiddenIO, config2.hiddenIO));
        
        StorageConfig config4 = new StorageConfig();
        config4.id = UUID.randomUUID().toString();
        config4.name = "DB1";
        config4.moduleClass = "org.sensorhub.persistence.FeatureStorage";
        config4.autoStart = true;
        config4.storagePath = "path/to/db";
        
        StorageConfig clone4 = (StorageConfig)config4.clone();
        assertTrue(clone4.id.equals(config4.id));
        assertTrue(clone4.name.equals(config4.name));
        assertTrue(clone4.moduleClass.equals(config4.moduleClass));
        assertTrue(clone4.autoStart = config4.autoStart);
    }
    
    
    @Test
    public void testGetInstalledModuleTypes()
    {
        System.out.println("\nAvailable Module Types");
        for (IModuleProvider moduleType: registry.getInstalledModuleTypes())
        {
            System.out.println(moduleType.getModuleName() + ": " +
                               moduleType.getModuleClass().getCanonicalName());
        } 
        
        System.out.println("\nAvailable Service Types");
        for (IModuleProvider moduleType: registry.getInstalledModuleTypes(IServiceModule.class))
        {
            System.out.println(moduleType.getModuleName() + ": " +
                               moduleType.getModuleClass().getCanonicalName());
        }
        
        System.out.println();
    }
    
    
    private ModuleConfig createConfModule1()
    {
        MyConfig1 conf = new MyConfig1();
        conf.moduleClass = DummyModule.class.getCanonicalName();
        conf.autoStart = true;
        conf.name = "Module1";
        conf.param1 = "text1";
        conf.param2 = 33;
        return conf;
    }
    
    
    private ModuleConfig createConfModule2(String dependencyID)
    {   
        MyConfig2 conf = new MyConfig2();
        conf.moduleClass = DummyModule.class.getCanonicalName();
        conf.autoStart = true;
        conf.name = "Module2";
        conf.param1 = "text2";
        conf.param2 = 0.3256;
        conf.moduleID = dependencyID;
        return conf;
    }   
    

    @Test
    public void testLoadModule() throws Exception
    {
        ModuleConfig config = createConfModule1();
        registry.loadModule(config);
        
        System.out.println(registry.getLoadedModules());
        assertEquals(1, registry.getLoadedModules().size());
        
        for (IModule<?> m: registry.getLoadedModules())
            assertTrue(m.getName().equals(config.name));
    }
    
    
    @Test
    public void testLoadModuleWithDependency() throws Exception
    {
        ModuleConfig conf1 = createConfModule1();
        registry.loadModule(conf1);
        
        // save and reset registry
        registry.saveModulesConfiguration();
        setup();
        
        ModuleConfig conf2 = createConfModule2(conf1.id);
        registry.loadModule(conf2);
        
        System.out.println(registry.getLoadedModules());
        assertEquals(2, registry.getLoadedModules().size());        
        
        int i = 0;
        for (IModule<?> m: registry.getLoadedModules())
        {
            if (i == 0)
                assertTrue(m.getName().equals(conf1.name));
            else
                assertTrue(m.getName().equals(conf2.name));
            i++;
        }
    }
    
    
    @After
    public void cleanup()
    {
        try
        {
            registry.shutdown(false, false);
            configFile.delete();
        }
        catch (SensorHubException e)
        {
        }
    }
}
