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

import java.util.Map;


/**
 * <p>
 * Interface for the overall module managers factory. This is a top level structure that has
 * to be called to get instances of module managers.
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 15, 2010
 */
public interface IModuleManagerFactory
{
    /**
     * Gets a reference to the manager instance handling the specified type of module.
     * The default manager is created if non have been started explicitely.
     * @param moduleType
     * @return The concrete manager instance
     */
    public <ModuleType extends IModule<?>> IModuleManager<ModuleType> getManager(Class<ModuleType> moduleType);
    
    
    /**
     * Starts a manager handling the specified type of module and that provides as much functionality
     * specified by the hints table as possible.
     * @param moduleType
     * @param hints
     * @return
     */
    public <ModuleType extends IModule<?>> IModuleManager<ModuleType> startManager(Class<ModuleType> moduleType, Map<String, Object> hints);
}
