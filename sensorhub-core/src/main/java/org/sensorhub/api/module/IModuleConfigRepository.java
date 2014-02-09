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

import java.util.List;


/**
 * <p>
 * Interface for the module configuration repository.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 3, 2013
 */
public interface IModuleConfigRepository
{
    
    /**
     * Retrieves the list of all modules configuration
     * @return
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
     * @param moduleID
     * @return
     */
    public ModuleConfig get(String moduleID);
    
    
    /**
     * Adds a module configuration to the repository
     * An exception will be thrown if a module with the same id already exist
     * @param config
     */
    public void add(ModuleConfig config);
    
    
    /**
     * Updates the module configuration with the given id in the repository
     * If the id does not exist, the configuration entry is added with the add method
     * @param config
     */
    public void update(ModuleConfig newConfig);
    
    
    /**
     * Removes the module configuration with the given id from the repository
     * @param moduleID
     */
    public void remove(String moduleID);
    
    
    /**
     * Closes the database and release all resources associated to it
     */
    public void close();
    
}
