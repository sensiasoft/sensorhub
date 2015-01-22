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

import java.io.Serializable;


/**
 * <p>
 * Base class to hold modules' configuration options
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 16, 2010
 */
public class ModuleConfig implements Serializable, Cloneable
{
    private static final long serialVersionUID = 2267529983474592096L;
    
    
    /**
     * Unique ID of the module. It must be unique within the SensorHub instance
     * and remain the same during the whole life-time of the module
     */
    public String id;
    
    
    /**
     * Name of module that this configuration is for
     */
    public String name;
    
    
    /**
     * Class implementing the module (to be instantiated)
     */
    public String moduleClass;
    
    
    /**
     * Used to enable/disable the module
     */
    public boolean enabled = false;
    
    
    public String getModuleIdString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append(name);
        buf.append(" (");
        buf.append(id);
        buf.append(')');
        return buf.toString();
    }
}
