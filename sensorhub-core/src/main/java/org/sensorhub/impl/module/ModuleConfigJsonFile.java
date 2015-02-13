/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.module;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.module.ModuleConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;


/**
 * <p>
 * Class providing access to the configuration database that is used to
 * persist all modules' configuration.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 3, 2013
 */
public class ModuleConfigJsonFile implements IModuleConfigRepository
{
    private static final String OBJ_CLASS_FIELD = "objClass";
    
    Map<String, ModuleConfig> configMap;
    Gson gson;
    File configFile;
    
    
    public ModuleConfigJsonFile(String moduleConfigPath)
    {
        configFile = new File(moduleConfigPath);
        configMap = new LinkedHashMap<String, ModuleConfig>();
        
        // init json serializer/deserializer
        final GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.disableHtmlEscaping();
        builder.serializeNulls();
        
        builder.registerTypeAdapter(List.class, new JsonSerializer<List<?>>() {
            @Override
            public JsonElement serialize(List<?> src, Type typeOfSrc, JsonSerializationContext context)
            {
                JsonArray array = new JsonArray();
                for (Object obj: src)
                {
                    JsonElement elt = context.serialize(obj);
                    if (elt.isJsonObject())
                    {
                        JsonObject jsonObj = new JsonObject();
                        JsonPrimitive s = new JsonPrimitive(obj.getClass().getCanonicalName());
                        jsonObj.add(OBJ_CLASS_FIELD, s);
                        
                        // copy existing properties
                        for (Entry<String, JsonElement> property: ((JsonObject)elt).entrySet())
                            jsonObj.add(property.getKey(), property.getValue());
                        
                        elt = jsonObj;
                    }
                    array.add(elt);
                }
                return array;
            }            
        });
        
        builder.registerTypeAdapter(List.class, new JsonDeserializer<List<?>>() {
            @Override
            public List<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
            {
                JsonArray array = (JsonArray)json;
                List<Object> list = new ArrayList<Object>(array.size());                
                for (JsonElement elt: array)
                {
                    if (elt.isJsonObject())
                    {
                        Object newObj;
                        JsonElement objClassField = ((JsonObject)elt).get(OBJ_CLASS_FIELD);
                        
                        if (objClassField != null)
                        {
                            Class<?> fieldClass;
                            try
                            {
                                fieldClass = Class.forName(objClassField.getAsString());
                                newObj = context.deserialize(elt, fieldClass);
                                ((JsonObject)elt).remove(OBJ_CLASS_FIELD);
                            }
                            catch (ClassNotFoundException e)
                            {
                                throw new RuntimeException(e);
                            }
                        }
                        else
                        {
                            newObj = context.deserialize(elt, ((ParameterizedType)typeOfT).getActualTypeArguments()[0]);
                        }
                        
                        list.add(newObj);
                    }
                }
                return list;
            }                    
        });
        
        gson = builder.create();
        readJSON();
    }
    
    
    @Override
    public List<ModuleConfig> getAllModulesConfigurations()
    {
        if (configMap.isEmpty())
            readJSON();        
        return new ArrayList<ModuleConfig>(configMap.values());
    }
    
    
    @Override
    public boolean contains(String moduleID)
    {
        return configMap.containsKey(moduleID);
    }
    

    @Override
    public synchronized ModuleConfig get(String moduleID)
    {
        ModuleConfig conf = configMap.get(moduleID);
        if (conf == null)
            throw new RuntimeException("No configuration found for module id " + moduleID);
        return conf;
    }


    @Override
    public synchronized void add(ModuleConfig config)
    {
        ModuleConfig conf = configMap.get(config.id);
        if (conf != null)
            throw new RuntimeException("Config file " + configFile.getAbsolutePath() + " for module " + config.name + " already exists");
        configMap.put(config.id, config);
        writeJSON();
    }
    
    
    @Override
    public synchronized void update(ModuleConfig config)
    {
        get(config.id);
        configMap.put(config.id, config);
        writeJSON();
    }
    
    
    @Override
    public synchronized void remove(String moduleID)
    {
        configMap.remove(moduleID);
        writeJSON();
    }
    
    
    @Override
    public synchronized void close()
    {
        // nothing to do
    }
    
    
    /*
     * Reads all modules configuration from the given JSON config file
     */
    private void readJSON()
    {
        FileReader reader = null;
        if (!configFile.exists())
            return;
        
        try
        {
            reader = new FileReader(configFile);
            
            Type collectionType = new TypeToken<List<ModuleConfig>>(){}.getType();
            JsonReader jsonReader = new JsonReader(reader);
            List<ModuleConfig> configList = gson.fromJson(jsonReader, collectionType);
            for (ModuleConfig config: configList)
                configMap.put(config.id, config);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error while parsing module config file " + configFile.getAbsolutePath(), e);
        }
        finally
        {
            try
            {
                if (reader != null)
                    reader.close();
            }
            catch (IOException e)
            {
            }
        }
    }
    
    
    private void writeJSON()
    {
        FileWriter writer = null;
        
        try
        {
            List<ModuleConfig> configList = getAllModulesConfigurations();
            Object[] jsonData = new Object[configList.size()*2];
            for (int i=0; i<configList.size(); i++)
            {
                ModuleConfig config = configList.get(i); 
                jsonData[i*2] = config.getClass().getCanonicalName();
                jsonData[i*2+1] = config;
            }
            
            // create new JSON file
            writer = new FileWriter(configFile);
            Type collectionType = new TypeToken<List<ModuleConfig>>(){}.getType();
            writer.append(gson.toJson(configList, collectionType));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error while writing JSON config file " + configFile.getAbsolutePath(), e);
        }
        finally
        {
            try
            {
                if (writer != null)
                    writer.close();
            }
            catch (IOException e)
            {
            }
        }
    }
}
