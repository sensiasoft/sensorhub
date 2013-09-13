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

import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.module.IModuleProvider;
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
        configFolder = new File("test/");
        configFolder.mkdirs();
        configDb = new ModuleConfigDatabaseJson(configFolder.getAbsolutePath());
        
        registry = ModuleRegistry.create(configDb);
    }
    
    
    @Test
    public void testGetInstalledModuleTypes()
    {
        for (IModuleProvider moduleType: registry.getInstalledModuleTypes())
        {
            System.out.println(moduleType.getModuleTypeName() + ": " +
                               moduleType.getModuleClass().getCanonicalName());
        }        
    }
    
    

    @Test
    public void testLoadModule()
    {
        
    }
    
    
    @After
    public void cleanup()
    {
        for (File f: configFolder.listFiles())
            f.delete();
        configFolder.delete();
    }
}
