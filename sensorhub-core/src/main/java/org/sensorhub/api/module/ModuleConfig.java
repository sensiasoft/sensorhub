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

import org.sensorhub.api.config.DisplayInfo;
import com.rits.cloning.Cloner;


/**
 * <p>
 * Base class to hold modules' configuration options
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 16, 2010
 */
public class ModuleConfig implements Cloneable
{   
    
    /**
     * Unique ID of the module. It must be unique within the SensorHub instance
     * and remain the same during the whole life-time of the module
     */
    @DisplayInfo(label="Module ID", desc="Unique local ID of the module")
    public String id;
    
    
    /**
     * Class implementing the module (will be instantiated when module is loaded)
     */
    @DisplayInfo(label="Module Class", desc="Module implementation class")
    public String moduleClass;
    
    
    /**
     * User chosen name for the module
     */
    @DisplayInfo(label="Module Name")
    public String name;
    
    
    /**
     * Reflects state of the module (enable/disable)
     */
    @DisplayInfo(label="Enable", desc="Module state (enabled or disabled)")
    public boolean enabled = false;


    @Override
    public ModuleConfig clone()
    {
        return new Cloner().deepClone(this);
    }
}
