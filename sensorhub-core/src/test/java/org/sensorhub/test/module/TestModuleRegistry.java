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

package org.sensorhub.test.module;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.impl.module.DummyModule;
import org.sensorhub.impl.module.ModuleConfigDatabaseJson;
import org.sensorhub.impl.module.ModuleRegistry;


public class TestModuleRegistry
{
    File configFolder;
    ModuleConfigDatabaseJson configDb;
    ModuleRegistry registry;
    
    
    @Before
    public void setup()
    {
        configFolder = new File("junit-test/");
        configFolder.mkdirs();
        configDb = new ModuleConfigDatabaseJson(configFolder.getAbsolutePath());        
        registry = new ModuleRegistry(configDb);
        registry.loadAllModules();
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
        conf.enabled = true;
        conf.name = "Module1";
        conf.param1 = "text1";
        conf.param2 = 33;
        return conf;
    }
    
    
    private ModuleConfig createConfModule2(String dependencyID)
    {   
        MyConfig2 conf = new MyConfig2();
        conf.moduleClass = DummyModule.class.getCanonicalName();
        conf.enabled = true;
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
        assertTrue(registry.getLoadedModules().get(0).getName().equals(config.name));
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
        assertTrue(registry.getLoadedModules().get(0).getName().equals(conf1.name));
        assertTrue(registry.getLoadedModules().get(1).getName().equals(conf2.name));
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
