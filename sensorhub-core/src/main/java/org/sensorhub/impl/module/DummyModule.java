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

import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.module.ModuleConfig;


public class DummyModule implements IModule<ModuleConfig>
{
    ModuleConfig config;            
    public boolean isEnabled() { return config.enabled; }
    public void init(ModuleConfig config) { this.config = config; }
    public void updateConfig(ModuleConfig config) { }
    public ModuleConfig getConfiguration() { return config; }
    public String getName() { return config.name; }
    public String getLocalID() { return null; }
    public void start() {};
    public void stop() {}
    public void saveState(IModuleStateSaver saver) {}
    public void loadState(IModuleStateLoader loader) {}
    public void cleanup() {}
    public void registerListener(IEventListener listener) {}
    public void unregisterListener(IEventListener listener) {}
}