/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;

import org.sensorhub.api.common.Event;
import org.sensorhub.api.module.ModuleEvent.Type;


/**
 * <p>
 * Event type generated at various times during a module's lifecycle
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2013
 */
public class ModuleEvent extends Event<Type>
{
    public enum Type
    {
        /**
         * after the module state has changed
         */
        STATE_CHANGED,
        
        /**
         * after the module configuration has been changed and accepted through updateConfig()
         */
        CONFIG_CHANGED,
        
        /**
         * Module connected (e.g. to remote device or service).<br/>
         * Some modules may never generate this event
         */
        CONNECTED,
        
        /**
         * Module disconnected (e.g. from remote device or service).<br/>
         * Some modules may never generate this event
         */
        DISCONNECTED,
        
        /**
         * when a new status message is published
         */
        STATUS,
        
        /**
         * when an error occurs during asynchronous module execution
         */
        ERROR,        
        
        /**
         * after module is loaded by registry
         */
        LOADED,
        
        /**
         * after module is unloaded from registry
         */
        UNLOADED,
        
        /**
         * after module is fully deleted (along with its configuration) 
         */
        DELETED
    }
    
    
    public enum ModuleState 
    {
        /**
         * after module class is first instantiated and added to the registry
         */
        LOADED,
        
        /**
         * when module asynchronous init has been requested
         */
        INITIALIZING,
        
        /**
         * after module was successfully initialized
         */
        INITIALIZED,
        
        /**
         * when module asynchronous start has been requested
         */
        STARTING,
        
        /**
         * after module was successfully started
         */
        STARTED,
        
        /**
         * when module asynchronous stop has been requested
         */
        STOPPING,
        
        /**
         * after module was successfully stopped
         */
        STOPPED;
    }
    
    
    protected ModuleState newState;
    protected Throwable error;
    protected String msg;
    
    
    public ModuleEvent(IModule<?> module, Type type)
    {
        this.timeStamp = System.currentTimeMillis();
        this.source = module;
        this.type = type;
        
        if (type == Type.STATE_CHANGED)
            this.newState = module.getCurrentState();
        if (type == Type.ERROR)
            this.error = module.getCurrentError();
    }
    
    
    public ModuleEvent(IModule<?> module, ModuleState newState)
    {
        this(module, Type.STATE_CHANGED);
        this.newState = newState;
    }
    
    
    public ModuleEvent(IModule<?> module, Throwable error)
    {
        this(module, Type.ERROR);
        this.error = error;
    }
    
    
    public ModuleEvent(IModule<?> module, String msg)
    {
        this(module, Type.STATUS);
        this.msg = msg;
    }
    
    
    public IModule<?> getModule()
    {
        return (IModule<?>)source;
    }
    
    
    public ModuleState getNewState()
    {
        return newState;
    }
    
    
    public Throwable getError()
    {
        return error;
    }
}
