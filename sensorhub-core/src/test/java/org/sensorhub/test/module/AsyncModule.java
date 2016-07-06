/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.module;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.common.EventBus;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.utils.MsgUtils;


public class AsyncModule extends AbstractModule<AsyncModuleConfig> implements IEventListener
{
    ModuleRegistry registry = SensorHub.getInstance().getModuleRegistry();
    ExecutorService exec = Executors.newSingleThreadExecutor();
    
    
    @Override
    public void requestInit(boolean force) throws SensorHubException
    {
        if (canInit(force))
        {
            Callable<Void> task = new Callable<Void>()
            {
                public Void call() throws Exception
                {
                    try
                    {
                        if (config.moduleIDNeededForInit != null)
                        {
                            AbstractModule<?> module = (AbstractModule<?>)registry.getModuleById(config.moduleIDNeededForInit);
                            
                            if (!config.useWaitLoopForInit)
                            {
                                SensorHub.getInstance().getEventBus().registerListener(config.moduleIDNeededForInit, EventBus.MAIN_TOPIC, AsyncModule.this);
                                return null;
                            }
                            else
                                module.waitForState(config.moduleStateNeededForInit, 0);                    
                        }
                        
                        try { Thread.sleep(config.initDelay); }
                        catch(InterruptedException e) {}
                        init();
                        
                        return null;
                    }
                    catch (Exception e)
                    {
                        reportError("Error during init", e);
                        throw e;
                    }
                }
            };
            
            try
            {
                if (config.useThreadForInit)
                    exec.submit(task);
                else
                    task.call();
            }
            catch (Exception e)
            {
                throw new SensorHubException(e.getMessage(), e.getCause());
            }
        }
    }


    @Override
    public void init() throws SensorHubException
    {   
        System.out.println("Running init() of " + MsgUtils.moduleString(this));
        try { Thread.sleep(config.initExecTime); }
        catch(InterruptedException e) {}
        
        setState(ModuleState.INITIALIZED);
    }
    
    
    @Override
    public void requestStart() throws SensorHubException
    {
        if (canStart())
        {
            Callable<Void> task = new Callable<Void>()
            {
                public Void call() throws Exception
                {
                    try
                    {
                        if (config.moduleIDNeededForStart != null)
                        {
                            AbstractModule<?> module = (AbstractModule<?>)registry.getModuleById(config.moduleIDNeededForStart);
                            
                            if (!config.useWaitLoopForStart)
                            {
                                SensorHub.getInstance().getEventBus().registerListener(config.moduleIDNeededForStart, EventBus.MAIN_TOPIC, AsyncModule.this);
                                return null;
                            }
                            else
                                module.waitForState(config.moduleStateNeededForStart, 0);                    
                        }
                        
                        try { Thread.sleep(config.startDelay); }
                        catch(InterruptedException e) {}
                        start();
                        
                        return null;
                    }
                    catch (Exception e)
                    {
                        reportError("Error during start", e);
                        throw e;
                    }
                }
            };
            
            try
            {
                if (config.useThreadForStart)
                    exec.submit(task);
                else
                    task.call();
            }
            catch (Exception e)
            {
                throw new SensorHubException(e.getMessage(), e.getCause());
            }     
        }
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        System.out.println("Running start() of " + MsgUtils.moduleString(this));
        try { Thread.sleep(config.startExecTime); }
        catch(InterruptedException e) {}
        
        setState(ModuleState.STARTED);
    }
    
    
    @Override
    public void requestStop() throws SensorHubException
    {
        if (canStop())
        {
            Callable<Void> task = new Callable<Void>()
            {
                public Void call() throws Exception
                {
                    try
                    {
                        try { Thread.sleep(config.stopDelay); }
                        catch(InterruptedException e) {}
                        stop();
                        
                        return null;
                    }
                    catch (Exception e)
                    {
                        reportError("Error during stop", e);
                        throw e;
                    }
                }
            };
            
            try
            {
                if (config.useThreadForStop)
                    exec.submit(task);
                else
                    task.call();
            }
            catch (Exception e)
            {
                throw new SensorHubException(e.getMessage(), e.getCause());
            }     
        }
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        System.out.println("Running stop() of " + MsgUtils.moduleString(this));
        try { Thread.sleep(config.stopExecTime); }
        catch(InterruptedException e) {}
        
        setState(ModuleState.STOPPED);        
    }


    @Override
    public void handleEvent(Event<?> e)
    {
        if (e instanceof ModuleEvent)
        {
            switch (((ModuleEvent)e).getType())
            {                
                case STATE_CHANGED:
                    IModule<?> module = (IModule<?>)e.getSource();
                    String moduleID = module.getLocalID();
                    ModuleState state = module.getCurrentState();
                    
                    if (moduleID.equals(config.moduleIDNeededForInit) && state == config.moduleStateNeededForInit)
                    {
                        try
                        {
                            init(this.config);
                        }
                        catch (SensorHubException e1)
                        {
                            reportError("Cannot init module", e1);
                        }
                    }
                    
                    else if (moduleID.equals(config.moduleIDNeededForStart) && state == config.moduleStateNeededForStart)
                    {
                        try
                        {
                            start();
                        }
                        catch (SensorHubException e1)
                        {
                            reportError("Cannot start module", e1);
                        }
                    }
                    
                    break;
                    
                default:
                    break;                
            }
        }        
    }

    
    @Override
    public void cleanup() throws SensorHubException
    {        
    }
}
