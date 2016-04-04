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

import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.module.ModuleEvent.Type;
import org.sensorhub.impl.common.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Class providing default implementation of common module API methods 
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ConfigType> 
 * @since Oct 30, 2014
 */
public abstract class AbstractModule<ConfigType extends ModuleConfig> implements IModule<ConfigType>
{
    protected Logger logger;
    protected IEventHandler eventHandler;
    protected ConfigType config;
    protected ModuleState state = ModuleState.LOADED;
    protected final Object stateLock = new Object();
    

    public AbstractModule()
    {
    }
    
    
    @Override
    public ConfigType getConfiguration()
    {
        return config;
    }


    @Override
    public String getName()
    {
        return config.name;
    }


    @Override
    public String getLocalID()
    {
        return config.id;
    }

    
    @Override
    public boolean isStarted()
    {
        return (state == ModuleState.STARTED);
    }
    
    
    @Override
    public ModuleState getCurrentState()
    {
        return state;
    }
    
    
    /**
     * Sets the module state and sends the appropriate event if it has changed
     * @param newState
     */
    protected void setState(ModuleState newState)
    {
        synchronized (stateLock)
        {
            if (newState != state)
            {
                this.state = newState;
                stateLock.notifyAll();
                
                ModuleEvent event = new ModuleEvent(this, Type.STATE_CHANGED);                
                eventHandler.publishEvent(event);
            }
        }
    }


    @Override
    public synchronized void init(ConfigType config) throws SensorHubException
    {
        this.config = config;
        this.eventHandler = EventBus.getInstance().registerProducer(config.id, EventBus.MAIN_TOPIC);
    }


    @Override
    public synchronized void updateConfig(ConfigType config) throws SensorHubException
    {
        boolean wasStarted = isStarted();
        
        // by default we restart the module when config was changed
        if (wasStarted)
            stop();
        
        this.config = config;
        
        if (wasStarted)
            start();
    }


    @Override
    public void saveState(IModuleStateManager saver) throws SensorHubException
    {
        // does nothing in the default implementation        
    }


    @Override
    public void loadState(IModuleStateManager loader) throws SensorHubException
    {
        // does nothing in the default implementation
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);        
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }
    
    
    protected Logger getLogger()
    {
        if (logger == null)
            logger = LoggerFactory.getLogger(this.getClass().getCanonicalName() + ":" + getLocalID().substring(0, 4));
        
        return logger;
    }
}
