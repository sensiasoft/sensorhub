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
import java.util.Timer;
import java.util.TimerTask;
import org.sensorhub.api.client.ClientConfig;
import org.sensorhub.api.comm.NetworkConfig;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.security.SecurityConfig;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.service.ServiceConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.common.EventBus;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.SensorSystem;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.ui.ModuleTypeSelectionPopup.ModuleTypeSelectionCallback;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.api.UIConstants;
import org.sensorhub.ui.data.MyBeanItem;
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
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.AbstractSelect.ItemDescriptionGenerator;
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
@Push(value=PushMode.MANUAL, transport=Transport.STREAMING)
public class AdminUI extends com.vaadin.ui.UI implements IEventListener, UIConstants
{
    private static final long serialVersionUID = 4069325051233125115L;    
    private static final Action ADD_MODULE_ACTION = new Action("Add Module", new ThemeResource("icons/module_add.png"));
    private static final Action REMOVE_MODULE_ACTION = new Action("Remove Module", new ThemeResource("icons/module_delete.png"));
    private static final Action START_MODULE_ACTION = new Action("Start", new ThemeResource("icons/enable.png"));
    private static final Action STOP_MODULE_ACTION = new Action("Stop", new ThemeResource("icons/disable.gif"));
    private static final Action RESTART_MODULE_ACTION = new Action("Restart", new ThemeResource("icons/refresh.gif"));
    private static final Action REINIT_MODULE_ACTION = new Action("Force Init", new ThemeResource("icons/refresh.gif"));
    private static final Resource LOGO_ICON = new ClassResource("/sensorhub_logo_128.png");
    private static final String STYLE_LOGO = "logo";
    private static final String PROP_STATE = "state";
    private static final String PROP_MODULE_OBJECT = "module";
    
    AdminUIConfig uiConfig;
    VerticalLayout configArea;
    Map<Class<?>, TreeTable> moduleTables = new HashMap<Class<?>, TreeTable>();
    
    
    @Override
    protected void init(VaadinRequest request)
    {
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
        
        // register new field converter for integer numbers
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
        leftPane.setSpacing(true);
        
        // header image and title
        Component header = buildHeader();
        leftPane.addComponent(header);
        leftPane.setExpandRatio(header, 0);
        
        // toolbar
        Component toolbar = buildToolbar();
        leftPane.addComponent(toolbar);
        leftPane.setExpandRatio(toolbar, 0);
        
        // accordion with several sections
        moduleTables.clear();
        Accordion stack = new Accordion();
        stack.setSizeFull();
        VerticalLayout layout;
        Tab tab;
                
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Sensors");
        //tab.setIcon(ACC_TAB_ICON);
        //tab.setIcon(FontAwesome.VIDEO_CAMERA);
        //tab.setIcon(FontAwesome.STETHOSCOPE);
        tab.setIcon(FontAwesome.RSS);
        buildModuleList(layout, SensorConfig.class);
        
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Storage");
        //tab.setIcon(ACC_TAB_ICON);
        tab.setIcon(FontAwesome.DATABASE);
        buildModuleList(layout, StorageConfig.class);
        
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Processing");
        //tab.setIcon(ACC_TAB_ICON);
        tab.setIcon(FontAwesome.GEARS);
        buildModuleList(layout, ProcessConfig.class);
        
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Services");
        //tab.setIcon(ACC_TAB_ICON);
        //tab.setIcon(FontAwesome.CLOUD_DOWNLOAD);
        //tab.setIcon(FontAwesome.CUBES);
        tab.setIcon(FontAwesome.TASKS);
        buildModuleList(layout, ServiceConfig.class);
        
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Clients");
        //tab.setIcon(ACC_TAB_ICON);
        tab.setIcon(FontAwesome.CLOUD_UPLOAD);
        buildModuleList(layout, ClientConfig.class);
        
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Network");
        //tab.setIcon(ACC_TAB_ICON);
        //tab.setIcon(FontAwesome.SIGNAL);
        tab.setIcon(FontAwesome.SITEMAP);
        buildNetworkModuleList(layout);
        
        layout = new VerticalLayout();
        tab = stack.addTab(layout, "Security");
        //tab.setIcon(ACC_TAB_ICON);
        tab.setIcon(FontAwesome.LOCK);
        buildModuleList(layout, SecurityConfig.class);
        
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
        header.setHeight(77.0f, Unit.PIXELS);
        header.setWidth(100.0f, Unit.PERCENTAGE);
        Image img = new Image(null, LOGO_ICON);
        img.setHeight(90, Unit.PIXELS);
        img.setStyleName(STYLE_LOGO);
        header.addComponent(img);
        Label title = new Label("SensorHub");
        title.addStyleName(STYLE_H1);
        title.addStyleName(STYLE_LOGO);
        title.setWidth(null);
        header.addComponent(title);
        header.setExpandRatio(img, 0);
        header.setExpandRatio(title, 1);
        header.setComponentAlignment(img, Alignment.MIDDLE_LEFT);
        header.setComponentAlignment(title, Alignment.MIDDLE_RIGHT);
        return header;
    }
    
    
    @SuppressWarnings("serial")
    protected Component buildToolbar()
    {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidth(100.0f, Unit.PERCENTAGE);
        toolbar.setSpacing(true);
                
        // shutdown button
        Button shutdownButton = new Button("Shutdown");
        shutdownButton.setDescription("Shutdown SensorHub");
        //shutdownButton.setIcon(DEL_ICON);
        shutdownButton.setIcon(FontAwesome.SIGN_OUT);
        shutdownButton.addStyleName(STYLE_SMALL);
        shutdownButton.setWidth(100.0f, Unit.PERCENTAGE);
        shutdownButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event)
            {
                final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to shutdown the sensor hub?");
                popup.addCloseListener(new CloseListener() {
                    @Override
                    public void windowClose(CloseEvent e)
                    {
                        if (popup.isConfirmed())
                        {                    
                            SensorHub.getInstance().getModuleRegistry().unregisterListener(AdminUI.this);
                            
                            Notification notif = new Notification(
                                    FontAwesome.WARNING.getHtml() + "&nbsp; Shutdown Initiated...",
                                    "UI will stop responding",
                                    Notification.Type.ERROR_MESSAGE);
                            notif.setHtmlContentAllowed(true);
                            notif.show(getPage());
                            
                            // shutdown in separate thread
                            new Timer().schedule(new TimerTask() {
                                public void run()
                                {
                                    SensorHub.getInstance().stop(false, true);
                                    System.exit(0);
                                }
                            }, 1000);
                        }
                    }                        
                });
                
                addWindow(popup);
            }
        });
        toolbar.addComponent(shutdownButton);
        
        // restart button
        Button restartButton = new Button("Restart");
        restartButton.setDescription("Restart SensorHub");
        restartButton.setIcon(REFRESH_ICON);
        restartButton.addStyleName(STYLE_SMALL);
        restartButton.setWidth(100.0f, Unit.PERCENTAGE);
        restartButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event)
            {
                final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to restart the sensor hub?");
                popup.addCloseListener(new CloseListener() {
                    @Override
                    public void windowClose(CloseEvent e)
                    {
                        if (popup.isConfirmed())
                        {                    
                            SensorHub.getInstance().getModuleRegistry().unregisterListener(AdminUI.this);
                            
                            Notification notif = new Notification(
                                    FontAwesome.WARNING.getHtml() + "&nbsp; Restart Initiated...",
                                    "UI will stop responding",
                                    Notification.Type.ERROR_MESSAGE);
                            notif.setHtmlContentAllowed(true);
                            notif.show(getPage());
                            
                            // shutdown in separate thread
                            new Timer().schedule(new TimerTask() {
                                public void run()
                                {
                                    SensorHub.getInstance().stop(false, true);
                                    System.exit(10); // return code 10 means restart
                                }
                            }, 1000);
                        }
                    }                        
                });
                
                addWindow(popup);
            }
        });
        toolbar.addComponent(restartButton);
        
        // apply changes button
        Button saveButton = new Button("Save");
        saveButton.setDescription("Save SensorHub Configuration");
        saveButton.setIcon(APPLY_ICON);
        saveButton.addStyleName(STYLE_SMALL);
        saveButton.setWidth(100.0f, Unit.PERCENTAGE);
        saveButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event)
            {
                final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to save the configuration (and override the previous one)?");
                popup.addCloseListener(new CloseListener() {
                    @Override
                    public void windowClose(CloseEvent e)
                    {
                        if (popup.isConfirmed())
                        {                    
                            try
                            {
                                SensorHub.getInstance().getModuleRegistry().saveModulesConfiguration();
                                DisplayUtils.showOperationSuccessful("SensorHub Configuration Saved");
                            }
                            catch (Exception ex)
                            {
                                Notification.show("Error", ex.getMessage(), Notification.Type.ERROR_MESSAGE);
                            }
                        }
                    }                        
                });
                
                addWindow(popup);
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
            if (config != null && NetworkConfig.class.isAssignableFrom(config.getClass()))
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
            if (config != null && configType.isAssignableFrom(config.getClass()))
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
        table.setNullSelectionAllowed(false);
        table.setImmediate(true);
        table.setColumnReorderingAllowed(false);
        table.addContainerProperty(PROP_NAME, String.class, PROP_NAME);
        table.addContainerProperty(PROP_STATE, ModuleState.class, ModuleState.LOADED);
        table.addContainerProperty(PROP_MODULE_OBJECT, IModule.class, null);
        table.setColumnWidth(PROP_STATE, 100);
        table.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        moduleTables.put(configType, table);
        
        // add modules info as table items       
        for (IModule<?> module: moduleList)
            addModuleToTable(module, table);
        
        // hide module object column!
        table.setVisibleColumns(PROP_NAME, PROP_STATE);
        
        // value converter for state field -> display as text and icon
        table.setConverter(PROP_STATE, new Converter<String, ModuleState>() {
            @Override
            public ModuleState convertToModel(String value, Class<? extends ModuleState> targetType, Locale locale)
            {
                return ModuleState.valueOf(value);
            }

            @Override
            public String convertToPresentation(ModuleState value, Class<? extends String> targetType, Locale locale)
            {
                return value.toString();
            }

            @Override
            public Class<ModuleState> getModelType()
            {
                return ModuleState.class;
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
                if (propertyId != null && propertyId.equals(PROP_STATE))
                {
                    ModuleState state = (ModuleState)table.getItem(itemId).getItemProperty(propertyId).getValue();
                    IModule<?> module = (IModule<?>)table.getItem(itemId).getItemProperty(PROP_MODULE_OBJECT).getValue();
                    Throwable error = module.getCurrentError();
                    
                    if (error == null)
                    {
                        if (state == ModuleState.STARTED)
                            return "green";
                        else
                            return "red";
                    }
                    else
                    {
                        return "error";
                    }
                }
                
                return null;
            }
        });
        
        table.setItemDescriptionGenerator(new ItemDescriptionGenerator() {                             
            public String generateDescription(Component source, Object itemId, Object propertyId) {
                if (propertyId != null && propertyId.equals(PROP_STATE))
                {
                    IModule<?> module = (IModule<?>)table.getItem(itemId).getItemProperty(PROP_MODULE_OBJECT).getValue();
                    Throwable error = module.getCurrentError();
                    if (error != null)
                    {
                        StringBuilder buf = new StringBuilder();
                        buf.append(error.getMessage());
                        if (error.getCause() != null)
                        {
                            buf.append("<br/>");
                            buf.append(error.getCause().getMessage());
                        }
                        return buf.toString();
                    }
                    else
                        return module.getStatusMessage();
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
                    // make sure row is selected even on right click
                    table.select(event.getItemId());
                    
                    // open bean item configuration
                    IModule<?> module = (IModule<?>)event.getItem().getItemProperty(PROP_MODULE_OBJECT).getValue();
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
                    ModuleState state = (ModuleState)table.getItem(target).getItemProperty(PROP_STATE).getValue();
                    if (state == ModuleState.STARTED)
                        actions.add(RESTART_MODULE_ACTION);
                    else
                        actions.add(START_MODULE_ACTION);
                    
                    actions.add(STOP_MODULE_ACTION);
                    actions.add(REINIT_MODULE_ACTION);
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
                
                // retrieve selected module
                final IModule<?> selectedModule;
                if (selectedId != null)
                    selectedModule = (IModule<?>)table.getItem(selectedId).getItemProperty(PROP_MODULE_OBJECT).getValue();
                else
                    selectedModule = null;
                
                if (action == ADD_MODULE_ACTION)
                {
                    // show popup to select among available module types
                    ModuleTypeSelectionPopup popup = new ModuleTypeSelectionPopup(configType, new ModuleTypeSelectionCallback() {
                        public void onSelected(Class<?> moduleType, ModuleConfig config)
                        {
                            IModule<?> module = null;
                            
                            try
                            {
                                // add to main config
                                module = registry.loadModule(config);
                            }
                            catch (Throwable e)
                            {
                                String msg = "The module could not be loaded";
                                Notification.show("Error", msg + '\n' + e.getMessage(), Notification.Type.ERROR_MESSAGE);
                                return;
                            }
                            
                            // no need to add module to table here
                            // it will be loaded when the LOADED event is received
                            
                            // show new module config panel
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
                    final String moduleName = (String)item.getItemProperty(PROP_NAME).getValue();
                    
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
                                        table.removeItem(selectedId);
                                        registry.destroyModule(moduleId);                                        
                                    }
                                    catch (SensorHubException ex)
                                    {                        
                                        String msg = "The module could not be removed";
                                        Notification.show("Error", msg + '\n' + ex.getMessage(), Notification.Type.ERROR_MESSAGE);
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
                                        if (selectedModule != null)
                                        {
                                            registry.startModuleAsync(selectedModule);
                                            openModuleInfo((MyBeanItem<ModuleConfig>)item, selectedModule);
                                        }
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
                                        if (selectedModule != null)
                                            registry.stopModuleAsync(selectedModule);
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
                                        if (selectedModule != null)
                                            registry.restartModuleAsync(selectedModule);
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
                    else if (action == REINIT_MODULE_ACTION)
                    {
                        final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to force re-init module " + moduleName + "?");
                        popup.addCloseListener(new CloseListener() {
                            @Override
                            public void windowClose(CloseEvent e)
                            {
                                if (popup.isConfirmed())
                                {                    
                                    try 
                                    {
                                        if (selectedModule != null)
                                            registry.initModuleAsync(selectedModule, true);
                                    }
                                    catch (SensorHubException ex)
                                    {
                                        String msg = "The module could not be reinitialized";
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
    
    
    protected void addModuleToTable(IModule<?> module, TreeTable table)
    {
        ModuleConfig config = module.getConfiguration();
                
        Item newItem = table.addItem(config.id);
        if (newItem == null) // in case module was already added
            return;
        
        newItem.getItemProperty(PROP_NAME).setValue(config.name);
        newItem.getItemProperty(PROP_STATE).setValue(module.getCurrentState());
        newItem.getItemProperty(PROP_MODULE_OBJECT).setValue(module);   
        
        // add submodules
        if (module instanceof SensorSystem)
        {
            for (ISensorModule<?> sensor: ((SensorSystem) module).getSensors().values())
            {
                ModuleConfig subConfig = sensor.getConfiguration();
                table.addItem(new Object[] {subConfig.name, sensor.getCurrentState(), sensor}, subConfig.id);
                table.setParent(subConfig.id, config.id);
            }
        }
        else
        {
            table.setChildrenAllowed(config.id, false);
        }
    }
        
    
    protected void openModuleInfo(MyBeanItem<ModuleConfig> beanItem, IModule<?> module)
    {
        configArea.removeAllComponents();
        
        // get panel for this config object        
        Class<?> configClass = beanItem.getBean().getClass();
        IModuleAdminPanel<IModule<?>> panel = AdminUIModule.getInstance().generatePanel(configClass);
        panel.build(beanItem, module);
        
        // generate module admin panel        
        configArea.addComponent(panel);
    }


    @Override
    public void handleEvent(final org.sensorhub.api.common.Event<?> e)
    {
        if (e instanceof ModuleEvent)
        {
            final IModule<?> module = (IModule<?>)e.getSource();
            final ModuleConfig config = module.getConfiguration();
            
            // find table and item corresponding to module
            TreeTable table = null;
            Item item = null;
            for (Class<?> configClass: moduleTables.keySet())
            {
                if (config != null && configClass.isAssignableFrom(config.getClass()))
                {
                    table = moduleTables.get(configClass);          
                    item = table.getItem(module.getLocalID());
                    break;
                }
            }
            
            // update table according to event type
            final TreeTable foundTable = table;
            final Item foundItem = item;
            switch (((ModuleEvent)e).getType())
            {
                case LOADED:
                    if (foundTable != null)
                    {
                        access(new Runnable() {
                            public void run()
                            {
                                // add module to table
                                addModuleToTable(module, foundTable);
                                push();
                            }
                        });
                    }
                    break;
                    
                case CONFIG_CHANGED:
                    if (foundItem != null)
                    {
                        access(new Runnable() {
                            public void run()
                            {
                                // update module name
                                foundItem.getItemProperty(PROP_NAME).setValue(config.name);
                                push();
                            }
                        });
                    }
                    break;
                    
                case STATE_CHANGED:
                case ERROR:
                    if (foundItem != null)
                    {
                        access(new Runnable() {
                            public void run()
                            {
                                // update module state
                                ModuleState state = ((IModule<?>)e.getSource()).getCurrentState();
                                foundItem.getItemProperty(PROP_STATE).setValue(state);
                                push();
                            }
                        });
                    }                    
                    break;                                
                    
                default:  
            }
        }     
    }


    @Override
    public void attach()
    {
        super.attach();
        
        // register to module registry events
        SensorHub.getInstance().getEventBus().registerListener(ModuleRegistry.ID, EventBus.MAIN_TOPIC, this);
    }
    
    
    @Override
    public void detach()
    {
        // unregister from module registry events
        SensorHub.getInstance().getEventBus().unregisterListener(ModuleRegistry.ID, EventBus.MAIN_TOPIC, this);
        
        super.detach();
    }

}
