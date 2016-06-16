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

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;


public class TestModuleRegistry
{
    ModuleRegistry registry;
    
    
    @Before
    public void setup()
    {
        System.out.println("\n*****************************");
        registry = SensorHub.getInstance().getModuleRegistry(); 
    }    
    
    
    private ModuleConfig createConfModule1()
    {
        MyConfig1 conf = new MyConfig1();
        conf.moduleClass = DummyModule.class.getCanonicalName();
        conf.id = "MOD1";
        conf.autoStart = true;
        conf.name = "Module1";
        conf.param1 = "text1";
        conf.param2 = 33;
        return conf;
    }
    
    
    private ModuleConfig createConfModule2()
    {   
        MyConfig2 conf = new MyConfig2();
        conf.moduleClass = DummyModule.class.getCanonicalName();
        conf.id = "MOD2";
        conf.autoStart = true;
        conf.name = "Module2";
        conf.param1 = "text2";
        conf.param2 = 0.3256;
        return conf;
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
    
    
    @Test
    public void testLoadModules() throws Exception
    {
        ModuleConfig config = createConfModule1();
        IModule<?> module = registry.loadModule(config);
        
        assertNotNull("Module instance is null", module);
        assertNotNull("Module configuration is null", module.getConfiguration());
        assertEquals("Registry size is wrong", 1, registry.getLoadedModules().size());
        assertEquals("Module name is wrong", registry.getModuleById(config.id).getName(), config.name);
        
        config = createConfModule2();
        module = registry.loadModule(config);
        
        assertNotNull("Module instance is null", module);
        assertNotNull("Module configuration is null", module.getConfiguration());
        assertEquals("Registry size is wrong", 2, registry.getLoadedModules().size());
        assertEquals("Module name is wrong", registry.getModuleById(config.id).getName(), config.name);
    }
    
    
    @Test
    public void testLoadModuleAsyncAutoStart() throws Exception
    {
        AsyncModuleConfig conf = new AsyncModuleConfig();
        conf.moduleClass = AsyncModule.class.getCanonicalName();
        conf.id = "MOD_ASYNC2";
        conf.autoStart = true;
        conf.name = "ModuleAsync2";
        conf.initDelay = 100;
        conf.initExecTime = 150;
        conf.startDelay = 200;
        conf.startExecTime = 250;        
        long timeOut = 10000;
        
        IModule<?> module = registry.loadModuleAsync(conf, null);
                
        long t0 = System.currentTimeMillis();
        module.waitForState(ModuleState.INITIALIZED, timeOut);
        long t1 = System.currentTimeMillis();
        module.waitForState(ModuleState.STARTED, timeOut);
        long t2 = System.currentTimeMillis();
        
        long expectedDelay = conf.initDelay + conf.initExecTime;
        long delay = t1 - t0;
        assertTrue("Init never executed", delay >= expectedDelay);
        assertTrue("Init timeout reached", delay < timeOut);
        
        expectedDelay = conf.startDelay + conf.startExecTime;
        delay = t2 - t1;
        assertTrue("Start never executed", delay >= expectedDelay);
        assertTrue("Start timeout reached", delay < timeOut);
    }
    
    
    @Test
    public void testInitModuleAsync() throws Exception
    {
        final AsyncModuleConfig conf = new AsyncModuleConfig();
        conf.moduleClass = AsyncModule.class.getCanonicalName();
        conf.id = "MOD_ASYNC2";
        conf.autoStart = false;
        conf.name = "ModuleAsync2";
        conf.initDelay = 100;
        conf.initExecTime = 200;
        long timeOut = 10000;
        
        IModule<?> module = registry.loadModule(conf);
        
        long t0 = System.currentTimeMillis();
        registry.initModuleAsync(conf.id, new IEventListener()
        {
            public void handleEvent(Event<?> e)
            {
                if (((ModuleEvent)e).getNewState() == ModuleState.INITIALIZED)
                    conf.initEventReceived = true;
            }            
        });
        module.waitForState(ModuleState.INITIALIZED, timeOut);
        long t1 = System.currentTimeMillis();
        
        long expectedDelay = conf.initDelay + conf.initExecTime;
        long delay = t1 - t0;
        assertTrue("Init never executed", delay >= expectedDelay);
        assertTrue("Init timeout reached", delay < timeOut);
        assertTrue("No INITIALIZED event received", conf.initEventReceived);
    }
    
    
    @Test
    public void testInitModuleAsyncTimeout() throws Exception
    {
        final AsyncModuleConfig conf = new AsyncModuleConfig();
        conf.moduleClass = AsyncModule.class.getCanonicalName();
        conf.id = "MOD_ASYNC2";
        conf.autoStart = false;
        conf.name = "ModuleAsync2";
        conf.initDelay = 100;
        conf.initExecTime = 200;
        long timeOut = 100;
        
        IModule<?> module = registry.loadModule(conf);
        registry.initModuleAsync(conf.id, null);
        boolean noTimeOut = module.waitForState(ModuleState.INITIALIZED, timeOut);
        
        assertFalse("Init timeout flag not set", noTimeOut);
    }
    
    
    @Test
    public void testStartModuleAsync() throws Exception
    {
        final AsyncModuleConfig conf = new AsyncModuleConfig();
        conf.moduleClass = AsyncModule.class.getCanonicalName();
        conf.id = "MOD_ASYNC2";
        conf.autoStart = false;
        conf.name = "ModuleAsync2";
        conf.initDelay = 100;
        conf.initExecTime = 150;
        conf.startDelay = 50;
        conf.startExecTime = 100;
        long timeOut = 10000;
        
        IModule<?> module = registry.loadModule(conf);
        
        long t0 = System.currentTimeMillis();
        registry.startModuleAsync(conf.id, new IEventListener()
        {
            public void handleEvent(Event<?> e)
            {
                if (((ModuleEvent)e).getNewState() == ModuleState.INITIALIZED)
                    conf.initEventReceived = true;
                else if (((ModuleEvent)e).getNewState() == ModuleState.STARTED)
                    conf.startEventReceived = true;
            }            
        });
        module.waitForState(ModuleState.INITIALIZED, timeOut);
        long t1 = System.currentTimeMillis();
        module.waitForState(ModuleState.STARTED, timeOut);
        long t2 = System.currentTimeMillis();
        
        long expectedDelay = conf.initDelay + conf.initExecTime;
        long delay = t1 - t0;
        assertTrue("Init never executed", delay >= expectedDelay);
        assertTrue("Init timeout reached", delay < timeOut);
        assertTrue("No INITIALIZED event received", conf.initEventReceived);
        
        expectedDelay = conf.startDelay + conf.startExecTime;
        delay = t2 - t1;
        assertTrue("Start never executed", delay >= expectedDelay);
        assertTrue("Start timeout reached", delay < timeOut);
        assertTrue("No STARTED event received", conf.startEventReceived);
    }
    
    
    @Test
    public void testStartModuleAsyncWithDependency() throws Exception
    {
        long timeOut = 2000;
        
        final AsyncModuleConfig conf1 = new AsyncModuleConfig();
        conf1.moduleClass = AsyncModule.class.getCanonicalName();
        conf1.id = "MOD_ASYNC1";
        conf1.autoStart = false;
        conf1.name = "ModuleAsync1";
        conf1.initDelay = 100;
        conf1.initExecTime = 150;
        conf1.startDelay = 50;
        conf1.startExecTime = 100;
        IModule<?> module1 = registry.loadModule(conf1);
        
        // configure module 2 to wait on module 1 before start
        final AsyncModuleConfig conf2 = new AsyncModuleConfig();
        conf2.moduleClass = AsyncModule.class.getCanonicalName();
        conf2.id = "MOD_ASYNC2";
        conf2.autoStart = false;
        conf2.name = "ModuleAsync2";
        conf2.initDelay = 10;
        conf2.initExecTime = 15;
        conf2.startDelay = 50;
        conf2.startExecTime = 10;
        conf2.moduleIDNeededForStart = conf1.id;
        conf2.moduleStateNeededForStart = ModuleState.STARTED;
        IModule<?> module2 = registry.loadModule(conf2);
        
        long t0 = System.currentTimeMillis();
        registry.startModuleAsync(conf2.id, new IEventListener()
        {
            public void handleEvent(Event<?> e)
            {
                if (((ModuleEvent)e).getNewState() == ModuleState.INITIALIZED)
                    conf2.initEventReceived = true;
                else if (((ModuleEvent)e).getNewState() == ModuleState.STARTED)
                    conf2.startEventReceived = true;
            }            
        });
        module2.waitForState(ModuleState.INITIALIZED, timeOut);
        long t1 = System.currentTimeMillis();
        module2.waitForState(ModuleState.STARTED, timeOut);
        long t2 = System.currentTimeMillis();
        
        // check module 2 has timed out on start
        long expectedDelay = conf2.initDelay + conf2.initExecTime;
        long delay = t1 - t0;
        assertTrue("Init never executed", delay >= expectedDelay);
        assertTrue("Init timeout reached", delay < timeOut);
        assertTrue("No INITIALIZED event received", conf2.initEventReceived);
        
        delay = t2 - t1;
        assertTrue("STARTED event should not have been received", !conf2.startEventReceived);
        assertTrue("Start timeout should have occured", delay >= timeOut);
        
        // now start module 1
        t0 = System.currentTimeMillis();
        registry.startModuleAsync(conf1.id, null);
        module1.waitForState(ModuleState.STARTED, timeOut);
        module2.waitForState(ModuleState.STARTED, timeOut);
        t1 = System.currentTimeMillis();
        
        expectedDelay = conf1.startDelay + conf1.startExecTime + conf2.startExecTime;
        delay = t1 - t0;
        assertTrue("Start never executed", delay >= expectedDelay);
        assertTrue("Start timeout reached", delay < timeOut);
        assertTrue("No STARTED event received", conf2.startEventReceived);        
    }
    
    
    @Test
    public void testRestartModuleAsync() throws Exception
    {
        final AsyncModuleConfig conf = new AsyncModuleConfig();
        conf.moduleClass = AsyncModule.class.getCanonicalName();
        conf.id = "MOD_ASYNC2";
        conf.autoStart = false;
        conf.name = "ModuleAsync2";
        conf.initDelay = 500;
        conf.initExecTime = 500;
        long timeOut = 10000;
        
        IModule<?> module = registry.loadModule(conf);
        
        // start module once
        registry.startModuleAsync(conf.id, null);
        module.waitForState(ModuleState.STARTED, timeOut);
        assertEquals("Module was not started", ModuleState.STARTED, module.getCurrentState());
        
        // now restart it
        long t0 = System.currentTimeMillis();
        registry.stopModuleAsync(conf.id, new IEventListener()
        {
            public void handleEvent(Event<?> e)
            {
                if (((ModuleEvent)e).getNewState() == ModuleState.STOPPED)
                    conf.stopEventReceived = true;
                else if (((ModuleEvent)e).getNewState() == ModuleState.STARTED)
                    conf.startEventReceived = true;
            }            
        });
        module.waitForState(ModuleState.STOPPING, timeOut);
        registry.startModuleAsync(conf.id, null);
        module.waitForState(ModuleState.STARTED, timeOut);
        long t1 = System.currentTimeMillis();
        
        long expectedDelay = conf.startDelay + conf.startExecTime;
        long delay = t1 - t0;
        assertTrue("No STOPPED event received", conf.stopEventReceived);
        assertTrue("Start never executed", delay >= expectedDelay);
        assertTrue("Start timeout reached", delay < timeOut);
        assertTrue("No STARTED event received", conf.startEventReceived);
    }
    
    
    @After
    public void cleanup()
    {
        try
        {
            registry.shutdown(false, false);
            SensorHub.clearInstance();
        }
        catch (SensorHubException e)
        {
        }
    }
}
