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

package org.sensorhub.api.module;

import org.sensorhub.api.common.Event;


/**
 * <p>
 * Event type generated at various times during a module's lifecycle
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2013
 */
public class ModuleEvent extends Event
{
    private static final long serialVersionUID = -6428469756344649559L;

    
    public enum Type 
    {
        LOADED,
        DELETED,
        DISABLED,
        ENABLED,
        CONFIG_CHANGE
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
        this.type = Type.CONFIG_CHANGE;
        this.newConfig = newConfig;
    } 
}
