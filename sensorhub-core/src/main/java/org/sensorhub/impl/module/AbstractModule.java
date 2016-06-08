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
import java.io.PrintWriter;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.common.EventBus;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.FileAppender;


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
    protected Throwable lastError;
    protected String statusMsg;
    

    public AbstractModule()
    {
    }
    
    
    @Override
    public void setConfiguration(ConfigType config)
    {
        if (this.config != config)
        {
            this.config = config;
            this.eventHandler = EventBus.getInstance().registerProducer(config.id, EventBus.MAIN_TOPIC);
        }
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
    public boolean isInitialized()
    {
        return (state.ordinal() >= ModuleState.INITIALIZED.ordinal());
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
                getLogger().info("Module " + newState);
                
                if (eventHandler != null)
                {
                    ModuleEvent event = new ModuleEvent(this, newState);
                    eventHandler.publishEvent(event);
                }
            }
        }
    }
    
    
    /**
     * Waits until the module reaches the specified state.<br/>
     * This method will return immediately if the state is already reached.
     * @param state state to wait for
     * @param timeout maximum time to wait in milliseconds or <= 0 to wait forever
     * @return true if module state has been reached before timeout, false otherwise
     */
    public boolean waitForState(ModuleState state, long timeout)
    {
        synchronized (stateLock)
        {
            try
            {
                long stopWait = System.currentTimeMillis() + timeout;
                while (this.state.ordinal() < state.ordinal())
                {
                    if (timeout > 0 && System.currentTimeMillis() > stopWait)
                        return false;
                    stateLock.wait(timeout/10);
                }
            }
            catch (InterruptedException e)
            {
                return false;
            }
            
            return true;
        }
    }
    
    
    @Override
    public Throwable getCurrentError()
    {
        return lastError;
    }
    
    
    /**
     * Sets the module error state
     * @param msg
     * @param error 
     */
    public void reportError(String msg, Throwable error)
    {
        if (error != this.lastError)
        {
            if (msg != null)
                this.lastError = new Exception(msg, error);
            else
                this.lastError = error;
            
            if (error != null)
            {
                if (eventHandler != null)
                {
                    ModuleEvent event = new ModuleEvent(this, this.lastError);               
                    eventHandler.publishEvent(event);
                }
                
                getLogger().error(msg, error);
            }
        }
    }
    
    
    public void clearError()
    {
        this.lastError = null;
    }
    
    
    @Override
    public String getStatusMessage()
    {
        return statusMsg;
    }
    
    
    /**
     * Sets the module status message
     * @param msg
     */
    public void reportStatus(String msg)
    {
        this.statusMsg = msg;
        getLogger().info(msg);
    }
    
    
    public void clearStatus()
    {
        this.statusMsg = null;
    }
    
    
    protected boolean canInit() throws SensorHubException
    {
        synchronized (stateLock)
        {
            if (state.ordinal() >= ModuleState.INITIALIZING.ordinal())
                return false;
            
            setState(ModuleState.INITIALIZING);            
            return true;
        }
    }
    
    
    @Override
    public void requestInit() throws SensorHubException
    {
        if (canInit())
        {
            // default implementation just calls init()
            init();
            setState(ModuleState.INITIALIZED);
        }
    }


    @Override
    public void init() throws SensorHubException
    {           
    }
    
    
    @Override
    public void init(ConfigType config) throws SensorHubException
    {   
        setConfiguration(config);
        init();
    }


    @Override
    public synchronized void updateConfig(ConfigType config) throws SensorHubException
    {
        boolean wasStarted = isStarted();
        
        // by default we restart the module when config was changed
        if (wasStarted)
            stop();
        
        this.config = config;
        eventHandler.publishEvent(new ModuleEvent(this, ModuleEvent.Type.CONFIG_CHANGED));
        
        if (wasStarted)
            start();
    }
    
    
    protected boolean canStart() throws SensorHubException
    {
        synchronized (stateLock)
        {
            if (this.state == ModuleState.STARTED || this.state == ModuleState.STARTING)
                return false;
            
            if (this.state != ModuleState.INITIALIZED && this.state != ModuleState.STOPPED)
                throw new SensorHubException("Module must be initialized first");
            
            setState(ModuleState.STARTING);
            return true;
        }
    }
    
    
    @Override
    public void requestStart() throws SensorHubException
    {
        if (canStart())
        {
            try
            {
                // default implementation just calls start()
                start();
                setState(ModuleState.STARTED);
            }
            catch (SensorHubException e)
            {
                reportError("Error while starting module", e);
                throw e;
            }
        }
    }
    
    
    protected boolean canStop() throws SensorHubException
    {
        synchronized (stateLock)
        {
            if (this.state != ModuleState.STARTED && this.state != ModuleState.STARTING)
                return false;
            
            setState(ModuleState.STOPPING);
            return true;
        }
    }
    
    
    @Override
    public void requestStop() throws SensorHubException
    {
        if (canStop())
        {
            try
            {
                // default implementation just calls stop()
                stop();
                setState(ModuleState.STOPPED);
            }
            catch (SensorHubException e)
            {
                reportError("Error while stopping module", e);
                throw e;
            }
        }
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
        synchronized (stateLock)
        {
            eventHandler.registerListener(listener);
            
            // notify current state synchronously while we're locked
            // so the listener can't miss it
            if (this.state != ModuleState.LOADED)
                listener.handleEvent(new ModuleEvent(this, this.state));
        }
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }
    
    
    public Logger getLogger()
    {
        if (logger == null)
        {
            // first create logger object
            String localID = getLocalID();
            String loggerId = Integer.toHexString(Math.abs(localID.hashCode()));
            logger = LoggerFactory.getLogger(this.getClass().getCanonicalName() + ":" + loggerId);
        
            if (logger instanceof ch.qos.logback.classic.Logger)
            {
                LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
                ch.qos.logback.classic.Logger logback = (ch.qos.logback.classic.Logger)logger;
                
                // configure to append to log file in module folder
                File moduleDataFolder = SensorHub.getInstance().getModuleRegistry().getModuleDataFolder(localID);
                File logFile = new File(moduleDataFolder, "log.txt");
                
                PatternLayoutEncoder ple = new PatternLayoutEncoder();
                ple.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] - %msg%n");
                ple.setContext(lc);
                ple.start();
                
                FileAppender<ILoggingEvent> fa = new FileAppender<ILoggingEvent>();
                fa.setFile(logFile.getAbsolutePath());
                fa.setEncoder(ple);
                fa.setAppend(true);
                fa.setContext(lc);
                fa.start(); 
                
                logback.addAppender(fa);
                logback.setLevel(Level.DEBUG);
                logback.setAdditive(true);
                
                // write initial messages on startup
                new PrintWriter(fa.getOutputStream()).println();
                fa.doAppend(new LoggingEvent(null, logback, Level.INFO, "", null, null));
                fa.doAppend(new LoggingEvent(null, logback, Level.INFO, "*************************************************", null, null));
                fa.doAppend(new LoggingEvent(null, logback, Level.INFO, "SensorHub Restarted", null, null));
                fa.doAppend(new LoggingEvent(null, logback, Level.INFO, "Starting log for " + MsgUtils.moduleString(this), null, null));
            }
        }
        
        return logger;
    }
}
