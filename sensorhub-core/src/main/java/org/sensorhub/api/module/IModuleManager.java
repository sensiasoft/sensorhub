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
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;

import java.lang.ref.WeakReference;
import java.util.List;


/**
 * <p>
 * Base interface for all module managers.
 * Module managers are used to manage all modules of the same type
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 12, 2010
 */
public interface IModuleManager<ModuleType extends IModule<?>>
{
    /**
     * Gets the list of all modules handled by this manager
     * that are already loaded (i.e. enabled)
     * @return
     */
    public List<ModuleType> getLoadedModules();
    
    
    /**
     * Gets the list of all modules handled by this manager
     * that are configured but not yet loaded (i.e. disabled)
     * @return
     */
    public List<ModuleConfig> getAvailableModules();
    
    
    /**
     * Retrieves a module instance by its local ID
     * @param moduleID Id of module to retrieve
     * @return direct reference to the module instance
     */
    public ModuleType getModuleById(String moduleID);

}
