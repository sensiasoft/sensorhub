/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import org.sensorhub.ui.api.IModuleConfigForm;
import org.sensorhub.ui.api.UIConstants;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;


public class TabbedConfigForms extends VerticalLayout implements UIConstants
{
    private static final long serialVersionUID = 2815909368923236139L;
    
    protected TabSheet configTabs;
    
    
    public TabbedConfigForms(IModuleConfigForm form)
    {
        setSizeUndefined();
        setWidth(100.0f, Unit.PERCENTAGE);
        
        Label sectionLabel = new Label("Configuration");
        sectionLabel.addStyleName(STYLE_H3);
        sectionLabel.addStyleName(STYLE_COLORED);
        addComponent(sectionLabel);
        
        // get all forms and put them into tabs
        configTabs = new TabSheet();
        configTabs.addTab(form, form.getCaption());
        for (Component subForm: form.getSubForms())
        {
            Tab tab = configTabs.addTab(subForm, subForm.getCaption());
            tab.setDescription(subForm.getDescription());
        }
        
        addComponent(configTabs);
    }
    
}
