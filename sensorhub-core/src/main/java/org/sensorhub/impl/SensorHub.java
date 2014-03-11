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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.config.IGlobalConfig;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.persistence.IPersistenceManager;
import org.sensorhub.api.sensor.ISensorManager;
import org.sensorhub.impl.module.ModuleConfigJsonFile;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.persistence.PersistenceManagerImpl;
import org.sensorhub.impl.sensor.SensorManagerImpl;


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
    private static final Log log = LogFactory.getLog(SensorHub.class);    
    private static final String ERROR_MSG = "Fatal error during sensorhub execution";
    private static SensorHub instance;
    
    private IGlobalConfig config;
    private ModuleRegistry registry;
    
    
    public static SensorHub createInstance(IGlobalConfig config)
    {
        if (instance == null)
            instance = new SensorHub(config);
        
        return instance;
    }
    
    
    public static SensorHub createInstance(IGlobalConfig config, ModuleRegistry registry)
    {
        if (instance == null)
            instance = new SensorHub(config, registry);
        
        return instance;
    }
    
    
    public static SensorHub getInstance()
    {
        return instance;
    }
    
    
    private SensorHub(IGlobalConfig config)
    {
        this.config = config;        
        IModuleConfigRepository configDB = new ModuleConfigJsonFile(config.getModuleConfigPath());
        this.registry = new ModuleRegistry(configDB);
    }
    
    
    private SensorHub(IGlobalConfig config, ModuleRegistry registry)
    {
        this.config = config;
        this.registry = registry;
    }
    
    
    public void start()
    {
        // load all modules in the order implied by dependency constraints
        registry.loadAllModules();
    }
    
    
    public void saveAndStop()
    {
        stop(true, true);
    }
    
    
    public void stop()
    {
        stop(false, false);
    }
    
    
    public void stop(boolean saveConfig, boolean saveState)
    {
        try
        {
            registry.shutdown(saveConfig, saveState);
        }
        catch (SensorHubException e)
        {
            log.error("Error while stopping SensorHub", e);
        }
    }
    
    
    public void registerListener(IEventListener listener)
    {
        registry.registerListener(listener);        
    }


    public void unregisterListener(IEventListener listener)
    {
        registry.unregisterListener(listener);        
    }
    
    
    public ModuleRegistry getModuleRegistry()
    {
        return registry;
    }
    
    
    public ISensorManager getSensorManager()
    {
        return new SensorManagerImpl(registry);
    }
    
    
    public IPersistenceManager getPersistenceManager()
    {
        return new PersistenceManagerImpl(registry, config.getBaseStoragePath());
    }
    
    
    public static void main(String[] args)
    {
        // if no arg provided
        if (args.length < 2)
        {
            // print usage
            System.out.println("SensorHub v1.0");
            System.out.println("Command syntax: sensorhub [module_config_path] [base_storage_path]");
            System.exit(1);
        }
        
        // else only argument is config path pointing to module config path
        SensorHub instance = null;
        try
        {
            SensorHubConfig config = new SensorHubConfig(args[0], args[1]);
            instance = SensorHub.createInstance(config);
            instance.start();
        }
        catch (Exception e)
        {
            if (instance != null)
                instance.stop();
            
            System.err.println(ERROR_MSG);
            System.err.println(e.getLocalizedMessage());
            log.error(ERROR_MSG, e);
            
            System.exit(2);
        }
    }
}
