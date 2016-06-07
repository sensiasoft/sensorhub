/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.module;

import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent.ModuleState;


public class DummyModule implements IModule<ModuleConfig>
{
    ModuleConfig config;
    public boolean isInitialized() { return true; }
    public boolean isStarted() { return true; }
    public void init(ModuleConfig config) { this.config = config; }
    public void updateConfig(ModuleConfig config) { }
    public void setConfiguration(ModuleConfig config) { this.config = config; }
    public ModuleConfig getConfiguration() { return config; }
    public String getName() { return config.name; }
    public String getLocalID() { return null; }
    public void start() {};
    public void stop() {}
    public void saveState(IModuleStateManager saver) {}
    public void loadState(IModuleStateManager loader) {}
    public void cleanup() {}
    public void registerListener(IEventListener listener) {}
    public void unregisterListener(IEventListener listener) {}
    public ModuleState getCurrentState() { return ModuleState.STARTED; }
    public String getStatusMessage() { return null; }
    public Throwable getCurrentError() { return null; }
    
    public void requestInit(ModuleConfig config) throws SensorHubException
    {
        init(config);
    }
    
    public void requestStart() throws SensorHubException
    {
        start();
    }

    public void requestStop() throws SensorHubException
    {
        stop();
    }   
    
}