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

import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.ui.api.IModuleConfigFormBuilder;
import org.sensorhub.ui.api.IModulePanelBuilder;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;


/**
 * <p>
 * Default implementation of module panel letting the user edit the module
 * configuration through a generic auto-generated form.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since 0.5
 */
public class DefaultModulePanelBuilder implements IModulePanelBuilder
{

    @Override
    public Component buildPanel(MyBeanItem<ModuleConfig> beanItem, IModule<?> module, IModuleConfigFormBuilder formBuilder)
    {
        // create panel with module name
        String moduleType = formBuilder.getTitle(beanItem.getBean());
        Panel panel = new Panel(moduleType);
                
        // add generated form
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeUndefined();
        layout.setWidth(100.0f, Unit.PERCENTAGE);
        layout.setMargin(true);
        
        Component form = formBuilder.buildForm(new FieldGroup(beanItem));
        layout.addComponent(form);
        
        panel.setContent(layout);
        return panel;
    }

}
