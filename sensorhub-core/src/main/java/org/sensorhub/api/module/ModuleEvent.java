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
         * after module class is first instantiated and init() has been called
         */
        LOADED,
        
        /**
         * after module is stopped and unloaded from registry
         */
        UNLOADED,
        
        /**
         * after module is fully deleted (along with its configuration) 
         */
        DELETED,
        
        /**
         * after module is enabled/started
         */
        STARTED,
        
        /**
         * before module is disabled/stopped
         */
        STOPPED,
        
        /**
         * after the module configuration has been changed and accepted through updateConfig()
         */
        CONFIG_CHANGED
    }
    
    
    public Type type;
    public ModuleConfig newConfig;
    
    
    public ModuleEvent(IModule<?> moduleInstance, Type type)
    {
        this.source = moduleInstance;
        this.type = type;
    }
    
    
    public ModuleEvent(IModule<?> moduleInstance, ModuleConfig newConfig)
    {
        this.source = moduleInstance;
        this.type = Type.CONFIG_CHANGED;
        this.newConfig = newConfig;
    } 
}
