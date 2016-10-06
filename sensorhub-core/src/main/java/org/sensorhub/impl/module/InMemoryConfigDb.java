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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.module.ModuleConfig;


/**
 * <p>
 * In memory config database used by JUnit tests
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 11, 2013
 */
public class InMemoryConfigDb implements IModuleConfigRepository
{
    Map<String, ModuleConfig> configMap = new LinkedHashMap<String, ModuleConfig>();
    
    
    @Override
    public List<ModuleConfig> getAllModulesConfigurations()
    {
        return new ArrayList<ModuleConfig>(configMap.values());
    }


    @Override
    public boolean contains(String moduleID)
    {
        return configMap.containsKey(moduleID);
    }


    @Override
    public ModuleConfig get(String moduleID)
    {
        return configMap.get(moduleID);
    }


    @Override
    public void add(ModuleConfig... configList)
    {
        for (ModuleConfig config: configList)
            configMap.put(config.id, config);
    }


    @Override
    public void update(ModuleConfig... configList)
    {
        for (ModuleConfig config: configList)
            configMap.put(config.id, config);
    }


    @Override
    public void remove(String... moduleIDs)
    {
        for (String moduleID: moduleIDs)
            configMap.remove(moduleID);
    }
    
    
    @Override
    public void commit()
    {
    }
    
    
    @Override
    public void close()
    {
        configMap.clear();
    }

}
