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
 
 Please Contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.module;

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