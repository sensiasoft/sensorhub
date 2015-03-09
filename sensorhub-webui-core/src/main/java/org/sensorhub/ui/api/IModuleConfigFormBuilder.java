/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui.api;

import org.sensorhub.api.module.ModuleConfig;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.ui.Component;


/**
 * <p>
 * Interface for all form panel used to configure modules
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since 0.5
 */
public interface IModuleConfigFormBuilder
{
    
    /**
     * Gets the title of the form
     * @param config instance
     * @return title string
     */
    public String getTitle(ModuleConfig config);
    
    
    /**
     * Builds the whole form for a given config object.
     * @param fieldGroup
     * @return root component of the generated form
     */
    public Component buildForm(FieldGroup fieldGroup);
    
}
