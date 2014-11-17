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
     * Gets the manager instance handling the specified type of module.
     * The default manager is created if non have been started explicitely.
     * @param moduleType
     * @return The concrete manager instance
     */
    public <ModuleType extends IModule<?>> IModuleManager<ModuleType> getManager(Class<ModuleType> moduleType);
    
    
    /**
     * Gets the manager instance handling the specified type of module and that provides
     * as much functionality specified by the hints table as possible.
     * @param moduleType
     * @param hints
     * @return module manager instance for the given module type
     */
    public <ModuleType extends IModule<?>> IModuleManager<ModuleType> getManager(Class<ModuleType> moduleType, Map<String, Object> hints);
}
