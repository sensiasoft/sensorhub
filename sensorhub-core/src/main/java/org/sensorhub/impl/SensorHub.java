/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Main class reponsible for starting/stopping all modules
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 4, 2013
 */
public class SensorHub
{
    private static final Logger log = LoggerFactory.getLogger(SensorHub.class);    
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
