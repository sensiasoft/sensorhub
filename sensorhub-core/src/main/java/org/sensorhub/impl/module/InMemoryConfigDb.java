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
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
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
    public void add(ModuleConfig config)
    {
        configMap.put(config.id, config);
    }


    @Override
    public void update(ModuleConfig newConfig)
    {
        configMap.put(newConfig.id, newConfig);
    }


    @Override
    public void remove(String moduleID)
    {
        configMap.remove(moduleID);
    }


    @Override
    public void close()
    {
        configMap.clear();
    }

}
