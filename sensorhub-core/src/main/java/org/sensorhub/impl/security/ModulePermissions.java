/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.security;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.SensorHub;
import org.vast.util.Asserts;
import com.rits.cloning.Cloner;


/**
 * <p>
 * Top level permission for a module.<br/>
 * This permission can match either a module ID or a module type alias.
 * (The module type alias is stored in the name attribute)
 * </p>
 *
 * @author Alex Robin
 * @since Aug 22, 2016
 */
public class ModulePermissions extends AbstractPermission
{
    String moduleTypeAlias;
    boolean isWildcard;
    
    
    public ModulePermissions(IModule<?> module, String moduleTypeAlias)
    {
        Asserts.checkNotNull(module);
        Asserts.checkNotNull(moduleTypeAlias);
        
        this.moduleTypeAlias = moduleTypeAlias;
        String idQualifier = "[" + module.getLocalID() + "]";
        this.name = moduleTypeAlias + idQualifier;
        this.label = module.getName() + " " + idQualifier;
    }
    
    
    public ModulePermissions(String moduleIdString)
    {
        super(null, moduleIdString);
        
        // extract module type alias and module ID
        int qualifierIndex = moduleIdString.indexOf('[');
        if (moduleIdString.length() < 4 || qualifierIndex < 1)
            throw new IllegalArgumentException("Invalid module identification string: " + moduleIdString);
        
        // extract module type alias
        this.moduleTypeAlias = moduleIdString.substring(0, qualifierIndex);
        
        // extract module ID
        String moduleID = moduleIdString.substring(qualifierIndex+1, moduleIdString.length()-1);
                
        if (IPermission.WILDCARD.equals(moduleID))
        {
            this.isWildcard = true;
        }
        else
        {
            // generate label 
            try
            {
                IModule<?> module = SensorHub.getInstance().getModuleRegistry().getModuleById(moduleID);
                this.label = module.getName() + " [" + getName() + "]";
            }
            catch (SensorHubException e)
            {
                this.label = "Unknown Module - " + moduleIdString;
            }
        }      
    }


    @Override
    public boolean implies(IPermission perm)
    {
        if (isWildcard && perm.getName().startsWith(moduleTypeAlias + '['))
            return true;
        
        if (name.equals(perm.getName()))
            return true;
        
        return false;
    }
    
    
    /**
     * Clones the permission tree to use as template for wildcard module permission
     * @param moduleTypeLabel
     * @return the template permission structure
     */
    public ModulePermissions cloneAsTemplatePermission(String moduleTypeLabel)
    {
        ModulePermissions templatePerm = new Cloner().deepClone(this);
        templatePerm.name = moduleTypeAlias + "[*]";
        templatePerm.label = "All " + moduleTypeLabel;
        templatePerm.isWildcard = true;
        return templatePerm;
    }
}
