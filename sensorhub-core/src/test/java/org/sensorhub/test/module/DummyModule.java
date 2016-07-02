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

import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.SensorHub;


public class DummyModule implements IModule<ModuleConfig>
{
    ModuleConfig config;
    ModuleState state = ModuleState.LOADED;
    IEventHandler eventHandler;


    public boolean isInitialized()
    {
        return true;
    }


    public boolean isStarted()
    {
        return (state == ModuleState.STARTED);
    }


    public void init()
    {
    }
    
    
    public void init(ModuleConfig config)
    {
        this.config = config;
        init();
    }


    public void updateConfig(ModuleConfig config)
    {
    }


    public void setConfiguration(ModuleConfig config)
    {
        this.config = config;
        this.eventHandler = SensorHub.getInstance().getEventBus().registerProducer(config.id);
    }


    public ModuleConfig getConfiguration()
    {
        return config;
    }


    public String getName()
    {
        return config.name;
    }


    public String getLocalID()
    {
        return config.id;
    }


    public void start()
    {
    }


    public void stop()
    {
    }


    public void saveState(IModuleStateManager saver)
    {
    }


    public void loadState(IModuleStateManager loader)
    {
    }


    public void cleanup()
    {
    }


    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);
    }


    public void unregisterListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);
    }


    public ModuleState getCurrentState()
    {
        return this.state;
    }


    public String getStatusMessage()
    {
        return null;
    }


    public Throwable getCurrentError()
    {
        return null;
    }


    public void requestInit() throws SensorHubException
    {
        init();
        setState(ModuleState.INITIALIZED);
    }


    public void requestStart() throws SensorHubException
    {
        start();
        setState(ModuleState.STARTED);
    }


    public void requestStop() throws SensorHubException
    {
        stop();
        setState(ModuleState.STOPPED);
    }
    
    
    protected void setState(ModuleState newState)
    {
        this.state = newState;
        eventHandler.publishEvent(new ModuleEvent(this, newState));
    }


    @Override
    public boolean waitForState(ModuleState state, long timeout)
    {
        return true;
    }

}