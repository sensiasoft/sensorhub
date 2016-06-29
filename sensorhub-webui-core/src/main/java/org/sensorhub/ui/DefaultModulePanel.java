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

import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.api.IModuleConfigForm;
import org.sensorhub.ui.api.UIConstants;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;


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
public class DefaultModulePanel<ModuleType extends IModule<? extends ModuleConfig>> extends VerticalLayout implements IModuleAdminPanel<ModuleType>, UIConstants, IEventListener
{
    private static final long serialVersionUID = -3391035886386668911L;
    ModuleType module;
    Button statusBtn;
    Button errorBtn;
    
    
    @Override
    @SuppressWarnings("serial")
    public void build(final MyBeanItem<ModuleConfig> beanItem, final ModuleType module)
    {
        this.module = module;
        
        setSizeUndefined();
        setWidth(100.0f, Unit.PERCENTAGE);
        setMargin(false);
        setSpacing(true);
        
        // module name
        String moduleName = beanItem.getBean().name;
        String className = beanItem.getBean().getClass().getSimpleName();
        Label title = new Label(moduleName);
        title.setDescription(className);
        title.setStyleName(STYLE_H2);
        addComponent(title);
        addComponent(new Label("<hr/>", ContentMode.HTML));
        
        // status message
        refreshStatusMessage();
        
        // error box
        refreshErrorMessage();
        
        // apply changes button
        Button applyButton = new Button("Apply Changes");
        applyButton.setIcon(APPLY_ICON);
        applyButton.addStyleName(STYLE_SMALL);
        applyButton.addStyleName("apply-button");
        addComponent(applyButton);
        
        // config forms
        final IModuleConfigForm form = getConfigForm(beanItem, module);
        addComponent(new TabbedConfigForms(form));
        
        // apply button action
        applyButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event)
            {
                try
                {
                    form.commit();
                    if (module != null)
                    {
                        SensorHub.getInstance().getModuleRegistry().updateModuleConfigAsync(beanItem.getBean());
                        DisplayUtils.showOperationSuccessful("Module Configuration Updated");
                    }
                }
                catch (Exception e)
                {
                    String msg = "Error while updating module configuration";
                    Page page = DefaultModulePanel.this.getUI().getPage();
                    new Notification("Error", msg + '\n' + e.getMessage(), Notification.Type.ERROR_MESSAGE).show(page);
                    AdminUIModule.log.error(msg, e);
                }
            }
        });
    }
    
    
    protected void refreshStatusMessage()
    {
        String statusMsg = module.getStatusMessage();
        if (statusMsg != null)
        {
            Button oldBtn = statusBtn;
            
            statusBtn = new Button();
            statusBtn.setStyleName(STYLE_LINK);
            statusBtn.setIcon(INFO_ICON);
            statusBtn.setCaption(statusMsg);
            
            if (oldBtn == null)
                addComponent(statusBtn, 2);
            else
                replaceComponent(oldBtn, statusBtn);
        }
        else
        {
            if (statusBtn != null)
            {
                removeComponent(statusBtn);
                statusBtn = null;
            }
        }
    }
    
    
    @SuppressWarnings("serial")
    protected void refreshErrorMessage()
    {
        final Throwable errorObj = module.getCurrentError();
        if (errorObj != null)
        {
            Button oldBtn = errorBtn;
            
            errorBtn = new Button();
            errorBtn.setStyleName(STYLE_LINK);
            errorBtn.setIcon(ERROR_ICON);
            String errorMsg = errorObj.getMessage().trim();
            if (!errorMsg.endsWith("."))
                errorMsg += ". ";
            if (errorObj.getCause() != null && errorObj.getCause().getMessage() != null)
                errorMsg += errorObj.getCause().getMessage();
            errorBtn.setCaption(errorMsg);
            
            errorBtn.addClickListener(new ClickListener() {
                public void buttonClick(ClickEvent event)
                {
                    /*StringWriter writer = new StringWriter();
                    errorObj.printStackTrace(new PrintWriter(writer));
                    String stackTrace = writer.toString();
                    stackTrace.replace("Caused By:", "\nCaused By:");
                    Notification.show("Error\n", writer.toString(), Notification.Type.ERROR_MESSAGE);*/
                    StringBuilder buf = new StringBuilder();
                    Throwable ex = errorObj;
                    do {
                        String msg = ex.getMessage();
                        if (msg != null)
                        {
                            msg = msg.trim();
                            buf.append(msg);
                            if (!msg.endsWith("."))
                                buf.append('.');
                            buf.append('\n');
                        }
                        ex = ex.getCause();
                    }
                    while (ex != null);
                    Notification.show("Error\n\n", buf.toString(), Notification.Type.ERROR_MESSAGE);
                }
            });
            
            if (oldBtn == null)
                addComponent(errorBtn, (statusBtn == null) ? 2 : 3);
            else
                replaceComponent(oldBtn, errorBtn);
        }
        else
        {
            if (errorBtn != null)
            {
                removeComponent(errorBtn);
                errorBtn = null;
            }
        }
    }
    
    
    protected IModuleConfigForm getConfigForm(MyBeanItem<ModuleConfig> beanItem, ModuleType module)
    {
        IModuleConfigForm form = AdminUIModule.getInstance().generateForm(beanItem.getBean().getClass());//module.getClass());
        //form.build("Main Settings", null, beanItem);
        form.build(GenericConfigForm.MAIN_CONFIG, "General module configuration", beanItem, false);
        return form;
    }
    
    
    @Override
    public void attach()
    {
        super.attach();
        module.registerListener(this);
    }
    
    
    @Override
    public void detach()
    {
        module.unregisterListener(this);
        super.detach();
    }


    @Override
    public void handleEvent(org.sensorhub.api.common.Event<?> e)
    {
        if (e instanceof ModuleEvent)
        {
            switch (((ModuleEvent)e).getType())
            {
                case STATUS:
                    getUI().access(new Runnable() {
                        public void run()
                        {
                            refreshStatusMessage();
                            getUI().push();
                        }
                    });                    
                    break;
                    
                case ERROR:
                    getUI().access(new Runnable() {
                        public void run()
                        {
                            refreshErrorMessage();
                            getUI().push();
                        }
                    });                    
                    break;
                    
                case STATE_CHANGED:
                    getUI().access(new Runnable() {
                        public void run()
                        {
                            refreshStatusMessage();
                            refreshErrorMessage();
                            getUI().push();
                        }
                    });  
                    break;
                    
                case CONFIG_CHANGED:
                    getUI().access(new Runnable() {
                        public void run()
                        {
                            DefaultModulePanel.this.removeAllComponents();
                            DefaultModulePanel.this.build(new MyBeanItem<ModuleConfig>(module.getConfiguration()), module);
                            getUI().push();
                        }
                    });  
                    break;
                    
                default:
                    return;
            }
        }        
    }

}
