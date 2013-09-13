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

package org.sensorhub.impl.module;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.module.ModuleConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;


/**
 * <p>
 * Class providing access to the configuration database that is used to
 * persist all modules' configuration.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin
 * @since Sep 3, 2013
 */
public class ModuleConfigDatabaseJson implements IModuleConfigRepository
{
    public static String JSON_EXT = ".json";
    
    Gson gson;
    File configFolder;
    
    
    public ModuleConfigDatabaseJson(String moduleConfigPath)
    {
        configFolder = new File(moduleConfigPath);
        
        // init json serializer/deserializer
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.disableHtmlEscaping();
        builder.serializeNulls();
        gson = builder.create();
    }
    
    
    @Override
    public List<ModuleConfig> getAllModulesConfigurations()
    {
        List<ModuleConfig> moduleConfigs = new ArrayList<ModuleConfig>();
        
        for (File f: configFolder.listFiles())
        {
            // parse each json file
            if (f.getName().endsWith(JSON_EXT))
                moduleConfigs.add(readConfig(f));
        }
        
        return moduleConfigs;
    }


    @Override
    public synchronized ModuleConfig get(String moduleID)
    {
        File configFile = getConfigFile(moduleID);
        if (!configFile.exists())
            throw new RuntimeException("No configuration found for module id " + moduleID);
        return readConfig(configFile);
    }


    @Override
    public synchronized void add(ModuleConfig config)
    {
        File configFile = getConfigFile(config.id);
        if (configFile.exists())
            throw new RuntimeException("Config file " + configFile.getAbsolutePath() + " for module " + config.name + " already exists");
        
        // create new JSON file
        Object[] jsonData = new Object[2];
        jsonData[0] = config.getClass().getCanonicalName();
        jsonData[1] = config;
        
        try
        {
            FileWriter writer = new FileWriter(configFile);
            writer.append(gson.toJson(jsonData));
            writer.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while writing JSON config file " + configFile.getAbsolutePath() + " for module " + config.name, e);
        }        
    }
    
    
    @Override
    public synchronized void update(ModuleConfig config)
    {
        File configFile = getConfigFile(config.id);
        if (configFile.exists())
            configFile.delete();
        add(config);
    }
    
    
    @Override
    public synchronized void remove(String moduleID)
    {
        File configFile = getConfigFile(moduleID);
        if (configFile.exists())
            configFile.delete();
    }
    
    
    /*
     * Build a file name and File object from the given id
     */
    private File getConfigFile(String moduleID)
    {
        return new File(configFolder, moduleID+JSON_EXT);
    }
    
    
    /*
     * Reads JSON from the given config file
     */
    private ModuleConfig readConfig(File configFile)
    {
        try
        {
            FileReader reader = new FileReader(configFile);
            JsonParser parser = new JsonParser();
            JsonArray array = parser.parse(reader).getAsJsonArray();
            
            // we first extract the class name to use (must extend ModuleConfig)
            String className = gson.fromJson(array.get(0), String.class);
            
            // and then parse object into the specified class
            ModuleConfig config = gson.fromJson(array.get(1), (Class<ModuleConfig>)Class.forName(className));            
            return config;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error while parsing module config file " + configFile.getAbsolutePath(), e);
        }
    }
    
}
