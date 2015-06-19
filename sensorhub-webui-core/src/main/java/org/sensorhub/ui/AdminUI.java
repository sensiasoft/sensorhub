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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.service.ServiceConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.persistence.StreamStorageConfig;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.ui.api.IModuleConfigFormBuilder;
import org.sensorhub.ui.api.IModulePanelBuilder;
import org.sensorhub.ui.data.MyBeanItem;
import org.sensorhub.ui.data.MyBeanItemContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.annotations.Theme;
import com.vaadin.data.Item;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.ClassResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;


//@Theme("reindeer")
@Theme("runo")
//@Theme("valo")
public class AdminUI extends com.vaadin.ui.UI
{
    private static final long serialVersionUID = 4069325051233125115L;
    private static Action ADD_MODULE_ACTION = new Action("Add Module", new ClassResource("/icons/module_add.png"));
    private static Action REMOVE_MODULE_ACTION = new Action("Remove Module", new ClassResource("/icons/module_delete.png"));
    private static Action ENABLE_MODULE_ACTION = new Action("Enable", new ClassResource("/icons/enable.png"));
    private static Action DISABLE_MODULE_ACTION = new Action("Disable", new ClassResource("/icons/disable.gif"));
    
    private static final Logger log = LoggerFactory.getLogger(AdminUI.class);
    
    VerticalLayout configArea;
    AdminUIConfig uiConfig;
    
    Map<Class<?>, MyBeanItemContainer<ModuleConfig>> moduleConfigLists = new HashMap<Class<?>, MyBeanItemContainer<ModuleConfig>>();
    Map<String, IModulePanelBuilder<?>> customPanels = new HashMap<String, IModulePanelBuilder<?>>();
    Map<String, IModuleConfigFormBuilder> customForms = new HashMap<String, IModuleConfigFormBuilder>();
    
    
    @Override
    protected void init(VaadinRequest request)
    {
        String configClass = null;
        
        // retrieve module config
        try
        {
            Properties initParams = request.getService().getDeploymentConfiguration().getInitParameters();
            String moduleID = initParams.getProperty(AdminUIModule.SERVLET_PARAM_MODULE_ID);
            uiConfig = (AdminUIConfig)SensorHub.getInstance().getModuleRegistry().getModuleById(moduleID).getConfiguration();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Cannot get UI module configuration", e);
        }
        
        try
        {
            // load default form builders        
            configClass = StreamStorageConfig.class.getCanonicalName();
            customForms.put(configClass, GenericStorageConfigForm.class.newInstance());
        
            // load custom form builders defined in config
            for (CustomPanelConfig customForm: uiConfig.customForms)
            {
                configClass = customForm.configClass;
                Class<?> clazz = Class.forName(customForm.builderClass);
                IModuleConfigFormBuilder formBuilder = (IModuleConfigFormBuilder)clazz.newInstance();
                customForms.put(configClass, formBuilder);
                log.debug("Loaded custom form for " + configClass);            
            }
        }
        catch (Exception e)
        {
            log.error("Error while instantiating form builder for config class " + configClass, e);
        }
        
        try
        {
            // load default panel builders
            configClass = SensorConfig.class.getCanonicalName();
            customPanels.put(configClass, SensorPanelBuilder.class.newInstance());        
        
            // load custom panel builders defined in config
            for (CustomPanelConfig customPanel: uiConfig.customPanels)
            {
                configClass = customPanel.configClass;
                Class<?> clazz = Class.forName(customPanel.builderClass);
                IModulePanelBuilder<?> panelBuilder = (IModulePanelBuilder<?>)clazz.newInstance();
                customPanels.put(configClass, panelBuilder);
                log.debug("Loaded custom panel for " + configClass);
            } 
        }
        catch (Exception e)
        {
            log.error("Error while instantiating panel builder for config class " + configClass, e);
        }
        
        // init main panels
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        splitPanel.setMinSplitPosition(300.0f, Unit.PIXELS);
        splitPanel.setMaxSplitPosition(80.0f, Unit.PERCENTAGE);
        splitPanel.setSplitPosition(500.0f, Unit.PIXELS);
        setContent(splitPanel);
        
        // build left stack
        Accordion stack = new Accordion();
        stack.setHeight(100.0f, Unit.PERCENTAGE);
        
        VerticalLayout layout = new VerticalLayout();
        stack.addTab(layout, "General");
        buildGeneralConfig(layout);
        
        layout = new VerticalLayout();
        stack.addTab(layout, "Sensors");
        buildModuleList(layout, SensorConfig.class);
        
        layout = new VerticalLayout();
        stack.addTab(layout, "Storage");
        buildModuleList(layout, StorageConfig.class);
        
        layout = new VerticalLayout();
        stack.addTab(layout, "Services");
        buildModuleList(layout, ServiceConfig.class);
        
        layout = new VerticalLayout();
        stack.addTab(layout, "Processing");
        buildModuleList(layout, ProcessConfig.class);
        
        layout = new VerticalLayout();
        stack.addTab(layout, "Network");
        splitPanel.addComponent(stack);
        
        // init config area
        configArea = new VerticalLayout();
        configArea.setMargin(true);
        splitPanel.addComponent(configArea);
    }
    
    
    protected void buildGeneralConfig(VerticalLayout layout)
    {
        // add config objects to container
        MyBeanItemContainer<ModuleConfig> container = new MyBeanItemContainer<ModuleConfig>(ModuleConfig.class);
        container.addBean(HttpServer.getInstance().getConfiguration());
        container.addBean(uiConfig); 
        displayModuleList(layout, container, null);
    }
    
    
    protected void buildModuleList(VerticalLayout layout, final Class<?> configType)
    {
        // add config objects to container
        List<ModuleConfig> moduleConfigs = SensorHub.getInstance().getModuleRegistry().getAvailableModules();
        MyBeanItemContainer<ModuleConfig> container = new MyBeanItemContainer<ModuleConfig>(ModuleConfig.class);
        for (ModuleConfig config: moduleConfigs)
        {
            if (configType.isAssignableFrom(config.getClass()))
                container.addBean(config);
        }
        
        moduleConfigLists.put(configType, container);
        displayModuleList(layout, container, configType);
    }
    
    
    @SuppressWarnings("serial")
    protected void displayModuleList(VerticalLayout layout, MyBeanItemContainer<ModuleConfig> container, final Class<?> configType)
    {
        // create table to display module list
        final Table table = new Table();
        table.setSizeFull();
        table.setSelectable(true);
        table.setImmediate(true);
        table.setColumnReorderingAllowed(false);
        table.setContainerDataSource(container);
        table.setVisibleColumns(new Object[] {"name", "enabled"});
        table.setColumnHeaders(new String[] {"Module Name", "Enabled"});
        
        // item click listener to display selected module settings
        table.addItemClickListener(new ItemClickListener()
        {
            @Override
            public void itemClick(ItemClickEvent event)
            {
                openModuleInfo((MyBeanItem<ModuleConfig>)event.getItem());
            }            
        });        
        layout.addComponent(table);
        
        // context menu
        table.addActionHandler(new Handler() {
            @Override
            public Action[] getActions(Object target, Object sender)
            {
                List<Action> actions = new ArrayList<Action>(10);
                                
                if (target != null)
                {                    
                    boolean enabled = ((MyBeanItem<ModuleConfig>)table.getItem(target)).getBean().enabled;
                    if (enabled)
                        actions.add(DISABLE_MODULE_ACTION);
                    else
                        actions.add(ENABLE_MODULE_ACTION);
                    actions.add(REMOVE_MODULE_ACTION);
                }
                else
                {
                    if (configType != null)
                        actions.add(ADD_MODULE_ACTION);
                }
                
                return actions.toArray(new Action[0]);
            }
            @Override
            public void handleAction(Action action, Object sender, Object target)
            {
                final Object selectedId = table.getValue();
                                
                if (action == ADD_MODULE_ACTION)
                {
                    // show popup to select among available module types
                    ModuleTypeSelectionPopup popup = new ModuleTypeSelectionPopup(AdminUI.this,  configType);
                    popup.setModal(true);
                    addWindow(popup);
                }
                
                else if (selectedId != null)
                {
                    // possible actions when a module is selected
                    final Item item = table.getItem(selectedId);
                    final String moduleId = (String)item.getItemProperty("id").getValue();
                    final String moduleName = (String)item.getItemProperty("name").getValue();
                    
                    if (action == REMOVE_MODULE_ACTION)
                    {
                        final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to remove module " + moduleName + "?</br>All settings will be lost.");
                        popup.addCloseListener(new CloseListener() {
                            @Override
                            public void windowClose(CloseEvent e)
                            {
                                if (popup.isConfirmed())
                                {                    
                                    try
                                    {
                                        SensorHub.getInstance().getModuleRegistry().destroyModule(moduleId);
                                        table.removeItem(selectedId);
                                    }
                                    catch (SensorHubException ex)
                                    {                        
                                        Notification.show("Error", "The module could not be removed", Notification.Type.ERROR_MESSAGE);
                                    }
                                }
                            }                        
                        });                    
                        
                        addWindow(popup);
                                           
                    }
                    else if (action == ENABLE_MODULE_ACTION)
                    {
                        final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to enable module " + moduleName + "?");
                        popup.addCloseListener(new CloseListener() {
                            @Override
                            public void windowClose(CloseEvent e)
                            {
                                if (popup.isConfirmed())
                                {                    
                                    try 
                                    {
                                        SensorHub.getInstance().getModuleRegistry().enableModule(moduleId);
                                        item.getItemProperty("enabled").setValue(true);
                                    }
                                    catch (SensorHubException ex)
                                    {
                                        Notification.show("Error", "The module could not be enabled", Notification.Type.ERROR_MESSAGE);
                                    }
                                }
                            }                        
                        });                    
                        
                        addWindow(popup);
                    }
                    else if (action == DISABLE_MODULE_ACTION)
                    {
                        final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to disable module " + moduleName + "?");
                        popup.addCloseListener(new CloseListener() {
                            @Override
                            public void windowClose(CloseEvent e)
                            {
                                if (popup.isConfirmed())
                                {                    
                                    try 
                                    {
                                        SensorHub.getInstance().getModuleRegistry().disableModule(moduleId);
                                        item.getItemProperty("enabled").setValue(false);
                                    }
                                    catch (SensorHubException ex)
                                    {
                                        Notification.show("Error", "The module could not be disabled", Notification.Type.ERROR_MESSAGE);
                                    }
                                }
                            }                        
                        });                    
                        
                        addWindow(popup);
                    }
                }
            }
        });
        
        layout.setSizeFull();
    }
    
    
    protected void openModuleInfo(MyBeanItem<ModuleConfig> beanItem)
    {
        configArea.removeAllComponents();
        
        Class<?> configClass = beanItem.getBean().getClass();
        Class<?> clazz;
        
        // TODO: do something different because getModuleById will load the module if not loaded yet
        // but here we don't necessarily need to load the module automatically
        IModule<?> module = null;                
        try { module = SensorHub.getInstance().getModuleRegistry().getModuleById(beanItem.getBean().id); }
        catch (Exception e) {}
        
        // check if there is a custom form registered, if not use default        
        IModuleConfigFormBuilder formBuilder = null;
        clazz = configClass;
        while (formBuilder == null && clazz != null)
        {
            formBuilder = customForms.get(clazz.getCanonicalName());
            clazz = clazz.getSuperclass();
        }
        if (formBuilder == null)
            formBuilder = new GenericConfigFormBuilder();
        
        // check if there is a custom panel registered, if not use default
        IModulePanelBuilder<IModule<?>> panelBuilder = null;
        clazz = configClass;
        while (panelBuilder == null && clazz != null)
        {
            panelBuilder = (IModulePanelBuilder<IModule<?>>)customPanels.get(clazz.getCanonicalName());
            clazz = clazz.getSuperclass();
        }
        if (panelBuilder == null)
            panelBuilder = new DefaultModulePanelBuilder<IModule<?>>();
        
        // generate module admin panel
        Component panel = panelBuilder.buildPanel(beanItem, module, formBuilder);
        configArea.addComponent(panel);
    }
    
    
    protected void addNewConfig(Class<?> moduleType, ModuleConfig config)
    {
        MyBeanItemContainer<ModuleConfig> container = moduleConfigLists.get(moduleType);
        MyBeanItem<ModuleConfig> newBeanItem = container.addBean(config);
        openModuleInfo(newBeanItem);
    }
}
