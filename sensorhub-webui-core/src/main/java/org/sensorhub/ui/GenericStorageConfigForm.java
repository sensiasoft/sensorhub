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

import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.ui.api.IModuleConfigForm;
import org.sensorhub.ui.data.ComplexProperty;
import com.vaadin.data.Property;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Field;


public class GenericStorageConfigForm extends GenericConfigForm implements IModuleConfigForm
{
    private static final long serialVersionUID = 4462720978742325333L;
    public static final String PROP_STORAGE_PATH = "storagePath";
    public static final String PROP_STORAGE_CONFIG = "storageConfig";
    
    
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop)
    {
        Field<?> field = super.buildAndBindField(label, propId, prop);
        
        if (propId.equals(PROP_STORAGE_PATH))
            field.setVisible(false);
        
        return field;
    }


    @Override
    protected ComponentContainer buildSubForm(String propId, ComplexProperty prop)
    {
        ComponentContainer subform = super.buildSubForm(propId, prop);
        
        if (prop.getValue() != null && propId.equals(PROP_STORAGE_CONFIG))
            addChangeButton(subform, propId, prop, StorageConfig.class);
        
        return subform;
    }

}
