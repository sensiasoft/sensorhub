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

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.LogManager;
import org.sensorhub.api.comm.CommProviderConfig;
import org.sensorhub.api.comm.NetworkConfig;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.persistence.StreamStorageConfig;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.api.IModuleConfigForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.server.VaadinServlet;


public class AdminUIModule extends AbstractModule<AdminUIConfig>
{
    protected static final Logger log = LoggerFactory.getLogger(AdminUI.class);
    protected final static String SERVLET_PARAM_UI_CLASS = "UI";
    protected final static String SERVLET_PARAM_MODULE_ID = "module_id";
    protected final static String WIDGETSET = "widgetset";
    
    private static AdminUIModule singleton;
    VaadinServlet vaadinServlet;
    Map<String, Class<? extends IModuleConfigForm>> customForms = new HashMap<String, Class<? extends IModuleConfigForm>>();
    Map<String, Class<? extends IModuleAdminPanel<?>>> customPanels = new HashMap<String, Class<? extends IModuleAdminPanel<?>>>();
    
    
    public AdminUIModule()
    {
        if (singleton != null)
            throw new IllegalStateException("Cannot create several AdminUI modules");
        
        singleton = this;
    }
    
    
    public static AdminUIModule getInstance()
    {
        return singleton;
    }
    
    
    @Override
    public void setConfiguration(AdminUIConfig config)
    {
        super.setConfiguration(config);
        String configClass = null;
        
        // load custom forms
        try
        {
            customForms.clear();
                    
            // default form builders
            customForms.put(HttpServerConfig.class.getCanonicalName(), HttpServerConfigForm.class);
            customForms.put(StreamStorageConfig.class.getCanonicalName(), GenericStorageConfigForm.class);
            customForms.put(CommProviderConfig.class.getCanonicalName(), CommProviderConfigForm.class);
            customForms.put(SOSConfigForm.SOS_PACKAGE + "SOSServiceConfig", SOSConfigForm.class);
            customForms.put(SPSConfigForm.SPS_PACKAGE + "SPSServiceConfig", SPSConfigForm.class);
            
            // custom form builders defined in config
            for (CustomUIConfig customForm: config.customForms)
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
        
        // load custom panels
        try
        {
            customPanels.clear();
            
            // load default panel builders
            customPanels.put(SensorConfig.class.getCanonicalName(), SensorAdminPanel.class);        
            customPanels.put(StorageConfig.class.getCanonicalName(), StorageAdminPanel.class);
            customPanels.put(NetworkConfig.class.getCanonicalName(), NetworkAdminPanel.class);
            
            // load custom panel builders defined in config
            for (CustomUIConfig customPanel: config.customPanels)
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
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        // reset java util logging config so we don't get annoying atmosphere logs
        LogManager.getLogManager().reset();//.getLogger("org.atmosphere").setLevel(Level.OFF);
        
        vaadinServlet = new VaadinServlet();
        Map<String, String> initParams = new HashMap<String, String>();
        initParams.put(SERVLET_PARAM_UI_CLASS, AdminUI.class.getCanonicalName());
        initParams.put(SERVLET_PARAM_MODULE_ID, getLocalID());
        if (config.widgetSet != null)
            initParams.put(WIDGETSET, config.widgetSet);
        initParams.put("productionMode", "true");  // set to false to compile theme on-the-fly
        
        // get HTTP server instance
        HttpServer httpServer = HttpServer.getInstance();
        if (!httpServer.waitForState(ModuleState.STARTED, 10000))
            throw new ServiceException("HTTP server hasn't started before timeout");
        
        // deploy servlet
        // HACK: we have to disable std err to hide message due to Vaadin duplicate implementation of SL4J
        // Note that this may hide error messages in other modules now that startup sequence is multithreaded
        PrintStream oldStdErr = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) { }
        }));
        httpServer.deployServlet(vaadinServlet, initParams, "/admin/*", "/VAADIN/*");
        System.setErr(oldStdErr);
        
        // setup security
        httpServer.addServletSecurity("/admin/*", "admin");
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        if (vaadinServlet != null)
            HttpServer.getInstance().undeployServlet(vaadinServlet);
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
    

    @Override
    public void cleanup() throws SensorHubException
    {        
    }

}
