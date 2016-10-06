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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.module.ModuleConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;


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
    
    
    /* GSON type adapter factory for parsing JSON object to a custom subclass.
     * The desired class is indicated by an additional field, whose name is
     * configured by typeFieldName. */
    public final class RuntimeTypeAdapterFactory<T> implements TypeAdapterFactory
    {
        private final Class<?> baseType;
        private final String typeFieldName;


        public RuntimeTypeAdapterFactory(Class<?> baseType, String typeFieldName)
        {
            if (typeFieldName == null || baseType == null)
                throw new NullPointerException();
            
            this.baseType = baseType;
            this.typeFieldName = typeFieldName;
        }


        public <R> TypeAdapter<R> create(final Gson gson, final TypeToken<R> type)
        {
            if (baseType != Object.class && !type.getRawType().isInstance(baseType))
                return null;
            
            return new TypeAdapter<R>()
            {
                @Override
                public R read(JsonReader in) throws IOException
                {
                    JsonElement jsonElement = Streams.parse(in);                
                    TypeAdapter<R> delegate = gson.getDelegateAdapter(RuntimeTypeAdapterFactory.this, type);
                    
                    if (jsonElement.isJsonObject())
                    {
                        JsonElement typeField = jsonElement.getAsJsonObject().remove(typeFieldName);
                                            
                        if (typeField != null)
                        {
                            String type = typeField.getAsString();
                            
                            try
                            {
                                Class<R> runtimeClass = (Class<R>)Class.forName(type);
                                delegate = gson.getDelegateAdapter(RuntimeTypeAdapterFactory.this, TypeToken.get(runtimeClass));                        
                            }
                            catch (ClassNotFoundException e)
                            {
                                throw new RuntimeException("Runtime class specified in JSON is invalid: " + type, e);
                            }
                        }
                    }
                    
                    return delegate.fromJsonTree(jsonElement);
                }


                @Override
                public void write(JsonWriter out, R value) throws IOException
                {
                    Class<R> runtimeClass = (Class<R>)value.getClass();
                    String typeName = runtimeClass.getName();
                    TypeAdapter<R> delegate = gson.getDelegateAdapter(RuntimeTypeAdapterFactory.this, TypeToken.get(runtimeClass));
                    JsonElement jsonElt = delegate.toJsonTree(value);
                    
                    if (jsonElt.isJsonObject())
                    {
                        JsonObject jsonObject = jsonElt.getAsJsonObject();
                        JsonObject clone = new JsonObject();
                        
                        // insert class name as first attribute
                        clone.add(typeFieldName, new JsonPrimitive(typeName));
                        for (Map.Entry<String, JsonElement> e : jsonObject.entrySet())
                            clone.add(e.getKey(), e.getValue());
                        
                        jsonElt = clone;
                    }
                    
                    Streams.write(jsonElt, out);
                }
            }.nullSafe();
        }
    }
        
    
    public ModuleConfigJsonFile(String moduleConfigPath)
    {
        configFile = new File(moduleConfigPath);
        configMap = new LinkedHashMap<String, ModuleConfig>();
        
        // init json serializer/deserializer
        final GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.disableHtmlEscaping();
        builder.registerTypeAdapterFactory(new RuntimeTypeAdapterFactory<Object>(Object.class, OBJ_CLASS_FIELD));
        
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
    public synchronized void add(ModuleConfig... configList)
    {
        for (ModuleConfig config: configList)
        {        
            ModuleConfig conf = configMap.get(config.id);
            if (conf != null)
                throw new RuntimeException("Module " + config.name + " already exists");            
            
            configMap.put(config.id, config);
        }
    }
    
    
    @Override
    public synchronized void update(ModuleConfig... configList)
    {
        for (ModuleConfig config: configList)
        {
            // generate a new ID if non was provided
            if (config.id == null)
                config.id = UUID.randomUUID().toString();
            
            configMap.put(config.id, config); 
        }
    }
    
    
    @Override
    public synchronized void remove(String... moduleIDs)
    {
        for (String moduleID: moduleIDs)
        {
            get(moduleID); // check if module exists
            configMap.remove(moduleID);
        }
    }
    
    
    @Override
    public synchronized void commit()
    {
        writeJSON();
    }
    
    
    @Override
    public synchronized void close()
    {
        commit();
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
            
            // build module map
            configMap.clear();
            for (ModuleConfig config: configList)
            {
                if (configMap.containsKey(config.id))
                    throw new RuntimeException("Duplicate module ID " + config.id);
                configMap.put(config.id, config);
            }
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
            Collection<ModuleConfig> configList = configMap.values();
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
