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

import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.ui.api.UIConstants;
import org.sensorhub.ui.data.ComplexProperty;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.data.Property;
import com.vaadin.ui.Field;


public class CommProviderConfigForm extends GenericConfigForm
{
    private static final long serialVersionUID = -5570947777524310604L;


    @Override
    public void build(String propId, ComplexProperty prop, boolean includeSubForms)
    {
        String title = prop.getLabel();
        if (title == null)
            title = "Communication Provider";
        
        String desc = prop.getDescription();
        if (desc == null)
            desc = "Settings for selected communication provider";
        
        if (prop.getValue() != null)
        {
            @SuppressWarnings("rawtypes")
            MyBeanItem beanItem = (MyBeanItem)prop.getValue().getItemProperty(propId+".protocol").getValue();
            Class<?> beanType = beanItem.getBean().getClass();
            title += " (" + beanType.getSimpleName().replace("Config", "") + ")";
        }
        
        build(title, desc, prop.getValue(), includeSubForms);
    }
    
    
    @Override
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop)
    {
        Field<?> field = super.buildAndBindField(label, propId, prop);
        
        if (propId.endsWith(UIConstants.PROP_ID))
            field.setVisible(false);
        else if (propId.endsWith(UIConstants.PROP_NAME))
            field.setVisible(false);
        else if (propId.endsWith(UIConstants.PROP_AUTOSTART))
            field.setVisible(false);
        else if (propId.endsWith(UIConstants.PROP_MODULECLASS))
            field.setCaption("Provider Class");
        
        return field;
    }
    
    
    @Override
    public Class<?> getPolymorphicBeanParentType()
    {
        return ICommProvider.class;
    }
}
