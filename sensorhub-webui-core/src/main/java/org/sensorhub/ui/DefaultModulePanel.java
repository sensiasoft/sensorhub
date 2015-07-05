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
import org.sensorhub.ui.api.IModuleConfigForm;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;


/**
 * <p>
 * Default implementation of module panel letting the user edit the module
 * configuration through a generic auto-generated form.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ModuleType> Type of module supported by this panel builder
 * @since 0.5
 */
public class DefaultModulePanel<ModuleType extends IModule<? extends ModuleConfig>> extends VerticalLayout implements IModuleAdminPanel<ModuleType>
{
    private static final long serialVersionUID = -3391035886386668911L;
    private static final Resource APPLY_ICON = new ThemeResource("icons/save.png");
    
    
    @Override
    @SuppressWarnings("serial")
    public void build(final MyBeanItem<ModuleConfig> beanItem, final ModuleType module)
    {
        setSizeUndefined();
        setWidth(100.0f, Unit.PERCENTAGE);
        setMargin(false);
        setSpacing(true);
        
        // title bar with save button
        HorizontalLayout titleBar = new HorizontalLayout();
        titleBar.setSizeFull();
        titleBar.setSpacing(true);
        
        // create label with module name
        String moduleName = getTitle(beanItem.getBean());
        Label title = new Label(moduleName);
        title.setStyleName(AdminUI.STYLE_H2);
        titleBar.addComponent(title);
        
        // apply changes button
        Button applyButton = new Button("Apply Changes");
        applyButton.setIcon(APPLY_ICON);
        titleBar.addComponent(applyButton);
        titleBar.setComponentAlignment(title, Alignment.MIDDLE_LEFT);
        titleBar.setComponentAlignment(applyButton, Alignment.MIDDLE_RIGHT);
        titleBar.setExpandRatio(title, 0.8f);
        titleBar.setExpandRatio(applyButton, 0.2f);
        addComponent(titleBar);
        
        // config form
        final IModuleConfigForm form = getConfigForm(beanItem, module);
        addComponent(form);
        
        // apply button action
        applyButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event)
            {
                try
                {
                    form.commit();
                    if (module != null)
                        ((IModule<ModuleConfig>)module).updateConfig(beanItem.getBean());
                }
                catch (Exception e)
                {
                    Notification.show("Error", e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }
        });
    }
    
    
    protected String getTitle(ModuleConfig config)
    {
        StringBuilder buf = new StringBuilder();
        buf.append(config.name);
        buf.append(" (");
        buf.append(config.getClass().getSimpleName());
        buf.append(')');
        return buf.toString();
    }
    
    
    protected IModuleConfigForm getConfigForm(MyBeanItem<ModuleConfig> beanItem, ModuleType module)
    {
        IModuleConfigForm form = AdminUI.getInstance().generateForm(beanItem.getBean().getClass());//module.getClass());
        form.build("Main Settings", beanItem);
        return form;
    }

}
