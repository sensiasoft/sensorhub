/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import org.sensorhub.ui.api.IModuleConfigForm;
import com.vaadin.data.Property;
import com.vaadin.ui.Field;


public class GenericStorageConfigForm extends GenericConfigForm implements IModuleConfigForm
{

    @Override
    protected void customizeField(String propId, Property<?> prop, Field<?> field)
    {
        super.customizeField(propId, prop, field);
        
        if (propId.equals("storagePath"))
            field.setVisible(false);
        
        else if (propId.equals("storageConfig.id"))
            field.setVisible(false);
        
        else if (propId.equals("storageConfig.name"))
            field.setVisible(false);
        
        else if (propId.equals("storageConfig.enabled"))
            field.setVisible(false);
        
        else if (propId.equals("storageConfig.moduleClass"))
            field.setVisible(false);
    }

}
