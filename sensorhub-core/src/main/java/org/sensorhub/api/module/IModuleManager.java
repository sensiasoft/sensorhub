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

import java.util.Collection;
import org.sensorhub.api.common.SensorHubException;


/**
 * <p>
 * Base interface for all module managers.
 * Module managers are used to manage all modules of the same type
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ModuleType> 
 * @since Nov 12, 2010
 */
public interface IModuleManager<ModuleType extends IModule<?>>
{
    /**
     * Gets the list of all modules handled by this manager
     * that are already loaded (i.e. enabled)
     * @return list of module instances
     */
    public Collection<ModuleType> getLoadedModules();
    
    
    /**
     * @param moduleID local ID of module  
     * @return true if module is loaded, false otherwise
     */
    public boolean isModuleLoaded(String moduleID);
    
    
    /**
     * Gets the list of all modules handled by this manager
     * that are configured but not yet loaded (i.e. disabled)
     * @return list of module configuration classes
     */
    public Collection<ModuleConfig> getAvailableModules();
    
    
    /**
     * Retrieves a module instance by its local ID
     * @param moduleID Id of module to retrieve
     * @return direct reference to the module instance
     * @throws SensorHubException if no module with given ID can be found
     */
    public ModuleType getModuleById(String moduleID) throws SensorHubException;

}
