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

import java.util.List;


/**
 * <p>
 * Interface for the module configuration repository.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 3, 2013
 */
public interface IModuleConfigRepository
{
    
    /**
     * @return list of all modules configuration
     */
    public List<ModuleConfig> getAllModulesConfigurations();
    
    
    /**
     * Checks if A module with the given ID exists
     * @param moduleID
     * @return true if the given ID exists in the database
     */
    public boolean contains(String moduleID);
    
    
    /**
     * Retrieves the configuration of the module with the given id
     * @param moduleID id of module
     * @return configuration of the module with the given id
     */
    public ModuleConfig get(String moduleID);
    
    
    /**
     * Adds a module configuration to the repository
     * An exception will be thrown if a module with the same id already exist
     * @param configList
     */
    public void add(ModuleConfig... configList);
    
    
    /**
     * Updates the module configuration with the given id in the repository
     * If the id does not exist, the configuration entry is added with the add method
     * @param configList
     */
    public void update(ModuleConfig... configList);
    
    
    /**
     * Removes the module configuration with the given id from the repository
     * @param moduleIDs
     */
    public void remove(String... moduleIDs);
    
    
    /**
     * Commits last configuration changes to the persistent store (if any)
     */
    public void commit();
    
    
    /**
     * Closes the module config registry and release all resources associated to it
     */
    public void close();
    
}
