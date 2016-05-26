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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.comm.NetworkConfig;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.service.ClientConfig;
import org.sensorhub.api.service.ServiceConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.persistence.StreamStorageConfig;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.ui.ModuleTypeSelectionPopup.ModuleTypeSelectionCallback;
import org.sensorhub.ui.api.IModuleConfigForm;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.api.UIConstants;
import org.sensorhub.ui.data.MyBeanItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.data.Item;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.converter.ConverterFactory;
import com.vaadin.data.util.converter.DefaultConverterFactory;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.ClassResource;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.CellStyleGenerator;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;


@Theme("sensorhub")
@Push(value=PushMode.MANUAL, transport=Transport.WEBSOCKET)
public class AdminUI extends com.vaadin.ui.UI
{
    private static final long serialVersionUID = 4069325051233125115L;
    
    private static final Action ADD_MODULE_ACTION = new Action("Add Module", new ThemeResource("icons/module_add.png"));
    private static final Action REMOVE_MODULE_ACTION = new Action("Remove Module", new ThemeResource("icons/module_delete.png"));
    private static final Action START_MODULE_ACTION = new Action("Start", new ThemeResource("icons/enable.png"));
    private static final Action STOP_MODULE_ACTION = new Action("Stop", new ThemeResource("icons/disable.gif"));
    private static final Action RESTART_MODULE_ACTION = new Action("Restart", new ThemeResource("icons/refresh.gif"));    
    private static final Resource LOGO_ICON = new ClassResource("/sensorhub_logo_128.png");
    private static final Resource ACC_TAB_ICON = new ThemeResource("icons/enable.png");    
    private static final String STYLE_LOGO = "logo";
    private static final String PROP_STARTED = "started";
    private static final String PROP_MODULE_OBJECT = "module";
    
    protected static final Logger log = LoggerFactory.getLogger(AdminUI.class);
    private static AdminUI singleton;
    
    
    VerticalLayout configArea;
    AdminUIConfig uiConfig;
    
    protected Map<String, Class<? extends IModuleAdminPanel<?>>> customPanels = new HashMap<String, Class<? extends IModuleAdminPanel<?>>>();
    protected Map<String, Class<? extends IModuleConfigForm>> customForms = new HashMap<String, Class<? extends IModuleConfigForm>>();
    
    
    public static AdminUI getInstance()
    {
        return singleton;
    }
    
    
    public AdminUI()
    {
        singleton = this;
    }
    
    
    @Override
    protected void init(VaadinRequest request)
    {
        String configClass = null;
        //moduleConfigLists.clear();
        
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
            customForms.put(HttpServerConfig.class.getCanonicalName(), HttpServerConfigForm.class);
            customForms.put(StreamStorageConfig.class.getCanonicalName(), GenericStorageConfigForm.class);
            customForms.put(CommConfig.class.getCanonicalName(), CommConfigForm.class);
            customForms.put(SOSConfigForm.SOS_PACKAGE + "SOSServiceConfig", SOSConfigForm.class);
            customForms.put(SOSConfigForm.SOS_PACKAGE + "SOSProviderConfig", SOSConfigForm.class);
            
            // load custom form builders defined in config
            for (CustomUIConfig customForm: uiConfig.customForms)
            {
                configClass = customForm.configClass;
                Class<?> clazz = Class.forName(customForm.uiClass);
                customForms.put(configClass, (Class<IModuleConfigForm>)clazz);
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
            customPanels.put(SensorConfig.class.getCanonicalName(), SensorAdminPanel.class);        
            customPanels.put(StorageConfig.class.getCanonicalName(), StorageAdminPanel.class);
            customPanels.put(NetworkConfig.class.getCanonicalName(), NetworkAdminPanel.class);
            
            // load custom panel builders defined in config
            for (CustomUIConfig customPanel: uiConfig.customPanels)
            {
                configClass = customPanel.configClass;
                Class<?> clazz = Class.forName(customPanel.uiClass);
                customPanels.put(configClass, (Class<IModuleAdminPanel<?>>)clazz);
                log.debug("Loaded custom panel for " + configClass);
            } 
        }
        catch (Exception e)
        {
            log.error("Error while instantiating panel builder for config class " + configClass, e);
        }
        
        // register new field converter for interger numbers
        @SuppressWarnings("serial")
        ConverterFactory converterFactory = new DefaultConverterFactory() {
            @Override
            protected <PRESENTATION, MODEL> Converter<PRESENTATION, MODEL> findConverter(
                    Class<PRESENTATION> presentationType, Class<MODEL> modelType) {
                // Handle String <-> Integer/Short/Long
                if (presentationType == String.class &&
                   (modelType == Long.class || modelType == Integer.class || modelType == Short.class )) {
                    return (Converter<PRESENTATION, MODEL>) new StringToIntegerConverter() {
                        @Override
                        protected NumberFormat getFormat(Locale locale) {
                            NumberFormat format = super.getFormat(Locale.US);
                            format.setGroupingUsed(false);
                            return format;
                        }
                    };
                }
                // Let default factory handle the rest
                return super.findConverter(presentationType, modelType);
            }
        };
        VaadinSession.getCurrent().setConverterFactory(converterFactory);
        
        // init main panels
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        splitPanel.setMinSplitPosition(300.0f, Unit.PIXELS);
        splitPanel.setMaxSplitPosition(30.0f, Unit.PERCENTAGE);
        splitPanel.setSplitPosition(350.0f, Unit.PIXELS);
        setContent(splitPanel);
        
        // build left pane
        VerticalLayout leftPane = new VerticalLayout();
        leftPane.setSizeFull();
        
        // header image and title
        Component header = buildHeader();
        leftPane.addComponent(header);
        leftPane.setExpandRatio(header, 0);
        
        // toolbar
        Component toolbar = buildToolbar();
        leftPane.addComponent(toolbar);
        leftPane.setExpandRatio(toolbar, 0);
        
        // accordion with several sections
        Accordion stack = new Accordion();
        stack.setSizeFull();
        VerticalLayout layout;
        Tab tab;
        
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Sensors");
        tab.setIcon(ACC_TAB_ICON);
        buildModuleList(layout, SensorConfig.class);
        
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Storage");
        tab.setIcon(ACC_TAB_ICON);
        buildModuleList(layout, StorageConfig.class);
        
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Processing");
        tab.setIcon(ACC_TAB_ICON);
        buildModuleList(layout, ProcessConfig.class);
        
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Services");
        tab.setIcon(ACC_TAB_ICON);
        buildModuleList(layout, ServiceConfig.class);
        
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Clients");
        tab.setIcon(ACC_TAB_ICON);
        buildModuleList(layout, ClientConfig.class);
        
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Network");
        tab.setIcon(ACC_TAB_ICON);
        buildNetworkModuleList(layout);
        
        leftPane.addComponent(stack);        
        leftPane.setExpandRatio(stack, 1);
        splitPanel.addComponent(leftPane);
        
        // init config area
        configArea = new VerticalLayout();
        configArea.setMargin(true);
        splitPanel.addComponent(configArea);
    }
    
    
    protected Component buildHeader()
    {
        HorizontalLayout header = new HorizontalLayout();
        header.setMargin(false);
        header.setHeight(100.0f, Unit.PIXELS);
        header.setWidth(100.0f, Unit.PERCENTAGE);
        Image img = new Image(null, LOGO_ICON);
        img.setHeight(90, Unit.PIXELS);
        img.setStyleName(STYLE_LOGO);
        header.addComponent(img);
        Label title = new Label("SensorHub");
        title.addStyleName(UIConstants.STYLE_H1);
        title.addStyleName(STYLE_LOGO);
        title.setWidth(null);
        header.addComponent(title);
        header.setExpandRatio(img, 0);
        header.setExpandRatio(title, 1);
        header.setComponentAlignment(img, Alignment.MIDDLE_LEFT);
        header.setComponentAlignment(title, Alignment.MIDDLE_RIGHT);
        return header;
    }
    
    
    protected Component buildToolbar()
    {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidth(100.0f, Unit.PERCENTAGE);
        
        // apply changes button
        Button saveButton = new Button("Save Config");
        saveButton.setDescription("Save Config");
        saveButton.setIcon(UIConstants.APPLY_ICON);
        saveButton.addStyleName(UIConstants.STYLE_SMALL);
        saveButton.setWidth(100.0f, Unit.PERCENTAGE);
        
        // apply button action
        saveButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = 1L;
            public void buttonClick(ClickEvent event)
            {
                try
                {
                    SensorHub.getInstance().getModuleRegistry().saveModulesConfiguration();
                }
                catch (Exception e)
                {
                    AdminUI.log.error("Error while saving SensorHub configuration", e);
                    Notification.show("Error", e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
            }
        });        

        toolbar.addComponent(saveButton);
        return toolbar;
    }
    
    
    protected void buildNetworkModuleList(VerticalLayout layout)
    {
        ModuleRegistry reg = SensorHub.getInstance().getModuleRegistry();
        ArrayList<IModule<?>> moduleList = new ArrayList<IModule<?>>();
        
        // add network modules to list
        moduleList.add(HttpServer.getInstance());
        for (IModule<?> module: reg.getLoadedModules())
        {
            ModuleConfig config = module.getConfiguration();
            if (NetworkConfig.class.isAssignableFrom(config.getClass()))
                moduleList.add(module);
        }        
        
        buildModuleList(layout, moduleList, NetworkConfig.class);
    }
    
    
    protected void buildModuleList(VerticalLayout layout, final Class<?> configType)
    {
        ModuleRegistry reg = SensorHub.getInstance().getModuleRegistry();
        ArrayList<IModule<?>> moduleList = new ArrayList<IModule<?>>();
        
        // add selected modules to list        
        for (IModule<?> module: reg.getLoadedModules())
        {
            ModuleConfig config = module.getConfiguration();
            if (configType.isAssignableFrom(config.getClass()))
                moduleList.add(module);
        }
        
        buildModuleList(layout, moduleList, configType);
    }
    
    
    @SuppressWarnings("serial")
    protected void buildModuleList(VerticalLayout layout, List<IModule<?>> moduleList, final Class<?> configType)
    {
        final ModuleRegistry registry = SensorHub.getInstance().getModuleRegistry();
        
        // create table to display module list
        final TreeTable table = new TreeTable();
        table.setSizeFull();
        table.setSelectable(true);
        table.setImmediate(true);
        table.setColumnReorderingAllowed(false);
        table.addContainerProperty(UIConstants.PROP_NAME, String.class, false);
        table.addContainerProperty(PROP_STARTED, Boolean.class, false);
        table.addContainerProperty(PROP_MODULE_OBJECT, IModule.class, null);
        table.setColumnWidth(PROP_STARTED, 100);
        table.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        
        // add modules info as table items       
        for (IModule<?> module: moduleList)
        {
            ModuleConfig config = module.getConfiguration();
            table.addItem(new Object[] {config.name, module.isStarted(), module}, config.id);
            
            // add submodules
            if (module instanceof SensorSystem)
            {
                for (ISensorModule<?> sensor: ((SensorSystem) module).getSensors().values())
                {
                    ModuleConfig subConfig = sensor.getConfiguration();
                    table.addItem(new Object[] {subConfig.name, sensor.isStarted(), sensor}, subConfig.id);
                    table.setParent(subConfig.id, config.id);
                }
            }
        }
        
        // hide module object column!
        table.setVisibleColumns(UIConstants.PROP_NAME, PROP_STARTED);
        
        // value converter for autostart field -> display as text and icon
        table.setConverter(PROP_STARTED, new Converter<String, Boolean>() {
            @Override
            public Boolean convertToModel(String value, Class<? extends Boolean> targetType, Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException
            {
                return (value != null && value.equals("Started"));
            }

            @Override
            public String convertToPresentation(Boolean value, Class<? extends String> targetType, Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException
            {
                return value ? "Started" : "Stopped";
            }

            @Override
            public Class<Boolean> getModelType()
            {
                return boolean.class;
            }

            @Override
            public Class<String> getPresentationType()
            {
                return String.class;
            }
        });
        
        table.setCellStyleGenerator(new CellStyleGenerator() {
            @Override
            public String getStyle(Table source, Object itemId, Object propertyId)
            {
                if (propertyId != null && propertyId.equals(PROP_STARTED))
                {
                    boolean val = (boolean)table.getItem(itemId).getItemProperty(propertyId).getValue();
                    if (val == true)
                        return "green";
                    else
                        return "red";
                }
                
                return null;
            }
        });
        
        // item click listener to display selected module settings
        table.addItemClickListener(new ItemClickListener()
        {
            @Override
            public void itemClick(ItemClickEvent event)
            {
                try
                {
                    IModule<?> module = (IModule<?>)event.getItem().getItemProperty(PROP_MODULE_OBJECT).getValue();
                    
                    // open bean item configuration
                    ModuleConfig config = module.getConfiguration().clone();
                    MyBeanItem<ModuleConfig> beanItem = new MyBeanItem<ModuleConfig>(config);
                    openModuleInfo(beanItem, module);
                }
                catch (Exception e)
                {
                    String msg = "Unexpected error when selecting module";
                    Notification.show("Error", msg + '\n' + e.getMessage(), Notification.Type.ERROR_MESSAGE);
                }
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
                    boolean started = (boolean)table.getItem(target).getItemProperty(PROP_STARTED).getValue();
                    if (started)
                    {
                        actions.add(STOP_MODULE_ACTION);
                        actions.add(RESTART_MODULE_ACTION);
                    }
                    else
                        actions.add(START_MODULE_ACTION);
                    
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
                    ModuleTypeSelectionPopup popup = new ModuleTypeSelectionPopup(configType, new ModuleTypeSelectionCallback() {
                        public void configSelected(Class<?> moduleType, ModuleConfig config)
                        {
                            IModule<?> module = null;
                            
                            try
                            {
                                // add to main config
                                module = registry.loadModule(config);
                            }
                            catch (Throwable e)
                            {
                                String msg = "The module could not be initialized";
                                Notification.show("Error", msg + '\n' + e.getMessage(), Notification.Type.ERROR_MESSAGE);
                                return;
                            }
                            
                            table.addItem(new Object[] {config.name, module.isStarted()}, config.id);
                            
                            // for some reason, setting the module in the addItem call prevents it to show up in the tree
                            // so we need to add it afterwards
                            table.getItem(config.id).getItemProperty(PROP_MODULE_OBJECT).setValue(module);
                            
                            MyBeanItem<ModuleConfig> newBeanItem = new MyBeanItem<ModuleConfig>(config);
                            openModuleInfo(newBeanItem, module);
                        }
                    });
                    popup.setModal(true);
                    addWindow(popup);
                }
                
                else if (selectedId != null)
                {
                    // possible actions when a module is selected
                    final Item item = table.getItem(selectedId);
                    final String moduleId = (String)selectedId;
                    final String moduleName = (String)item.getItemProperty(UIConstants.PROP_NAME).getValue();
                    
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
                                        registry.destroyModule(moduleId);
                                        table.removeItem(selectedId);
                                    }
                                    catch (SensorHubException ex)
                                    {                        
                                        String msg = "The module could not be removed";
                                        Notification.show("Error", msg + '\n' + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
                                        AdminUI.log.debug(msg, e);
                                    }
                                }
                            }                        
                        });                    
                        
                        addWindow(popup);
                                           
                    }
                    else if (action == START_MODULE_ACTION)
                    {
                        final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to start module " + moduleName + "?");
                        popup.addCloseListener(new CloseListener() {
                            @Override
                            public void windowClose(CloseEvent e)
                            {
                                if (popup.isConfirmed())
                                {                    
                                    try 
                                    {
                                        IModule<?> module = registry.startModule(moduleId);
                                        item.getItemProperty(PROP_STARTED).setValue(true);
                                        openModuleInfo((MyBeanItem<ModuleConfig>)item, module);
                                    }
                                    catch (SensorHubException ex)
                                    {
                                        String msg = "The module could not be started";
                                        Notification.show("Error", msg + '\n' + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
                                    }
                                }
                            }                        
                        });                    
                        
                        addWindow(popup);
                    }
                    else if (action == STOP_MODULE_ACTION)
                    {
                        final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to stop module " + moduleName + "?");
                        popup.addCloseListener(new CloseListener() {
                            @Override
                            public void windowClose(CloseEvent e)
                            {
                                if (popup.isConfirmed())
                                {                    
                                    try 
                                    {
                                        registry.stopModule(moduleId);
                                        item.getItemProperty(PROP_STARTED).setValue(false);
                                    }
                                    catch (SensorHubException ex)
                                    {
                                        String msg = "The module could not be stopped";
                                        Notification.show("Error", msg + '\n' + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
                                    }
                                }
                            }                        
                        });                    
                        
                        addWindow(popup);
                    }
                    else if (action == RESTART_MODULE_ACTION)
                    {
                        final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to restart module " + moduleName + "?");
                        popup.addCloseListener(new CloseListener() {
                            @Override
                            public void windowClose(CloseEvent e)
                            {
                                if (popup.isConfirmed())
                                {                    
                                    try 
                                    {
                                        SensorHub.getInstance().getModuleRegistry().stopModule(moduleId);
                                        SensorHub.getInstance().getModuleRegistry().startModule(moduleId);
                                        item.getItemProperty(PROP_STARTED).setValue(true);
                                    }
                                    catch (SensorHubException ex)
                                    {
                                        String msg = "The module could not be restarted";
                                        Notification.show("Error", msg + '\n' + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
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
        
    
    protected void openModuleInfo(MyBeanItem<ModuleConfig> beanItem, IModule<?> module)
    {
        configArea.removeAllComponents();
        
        // get panel for this config object        
        Class<?> configClass = beanItem.getBean().getClass();
        IModuleAdminPanel<IModule<?>> panel = generatePanel(configClass);
        panel.build(beanItem, module);
        
        // generate module admin panel        
        configArea.addComponent(panel);
    }
    
    
    protected IModuleAdminPanel<IModule<?>> generatePanel(Class<?> clazz)
    {
        try
        {
            // check if there is a custom panel registered, if not use default
            Class<IModuleAdminPanel<IModule<?>>> uiClass = null;
            while (uiClass == null && clazz != null)
            {
                uiClass = (Class<IModuleAdminPanel<IModule<?>>>)customPanels.get(clazz.getCanonicalName());
                clazz = clazz.getSuperclass();
            }
            
            if (uiClass == null)
                return new DefaultModulePanel<IModule<?>>();
            
            return uiClass.newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Cannot instantiate UI class", e);
        }
    }
    
    
    protected IModuleConfigForm generateForm(Class<?> clazz)
    {
        try
        {
            // check if there is a custom form registered, if not use default        
            Class<IModuleConfigForm> uiClass = null;
            while (uiClass == null && clazz != null)
            {
                uiClass = (Class<IModuleConfigForm>)customForms.get(clazz.getCanonicalName());
                clazz = clazz.getSuperclass();
            }
            
            if (uiClass == null)
                return new GenericConfigForm();
            
            return uiClass.newInstance();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Cannot instantiate UI class", e);
        }
    }

}
