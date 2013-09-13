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

package org.sensorhub.impl;

import org.sensorhub.api.config.IGlobalConfig;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.impl.module.ModuleConfigDatabaseJson;
import org.sensorhub.impl.module.ModuleRegistry;


/**
 * <p>
 * Main class reponsible for starting/stopping all modules
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 4, 2013
 */
public class SensorHub
{
    private static SensorHub instance;
    
    private IGlobalConfig config;
    
    
    public static SensorHub getInstance(IGlobalConfig config)
    {
        if (instance == null)
            instance = new SensorHub(config);
        
        return instance;
    }
    
    
    private SensorHub(IGlobalConfig config)
    {
        this.config = config;
    }
    
    
    public void start()
    {
        IModuleConfigRepository configDB = new ModuleConfigDatabaseJson(config.getModuleConfigPath());
        ModuleRegistry.create(configDB);
        
        // load all modules in the order implied by dependency constraints
        
    }
    
    
    public void stop()
    {
        
    }

}
