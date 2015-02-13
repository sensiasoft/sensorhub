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


/**
 * <p>
 * Interface to be implemented to enable automatic module discovery
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public interface IModuleProvider
{
    public String getModuleName();
    
    public String getModuleDescription();
    
    public String getModuleVersion();
    
    public String getProviderName();
    
    public Class<? extends IModule<?>> getModuleClass();
    
    public Class<? extends ModuleConfig> getModuleConfigClass();    
}
