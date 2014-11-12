/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * Wrapper module for the HTTP server engine (Jetty for now)
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 6, 2013
 */
public class HttpServer extends AbstractModule<HttpServerConfig>
{
    private static final Log log = LogFactory.getLog(HttpServer.class);
    public static String TEST_MSG = "SensorHub web server is up";
    private static HttpServer instance;
        
    Server server;
    ServletContextHandler servletHandler;
    
    
    public HttpServer()
    {
        if (instance != null)
            throw new RuntimeException("Cannot start several HTTP server instances");
        
        instance = this;
        
        // create servlet handler
        this.servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        
        // add default test servlet
        servletHandler.addServlet(new ServletHolder(new HttpServlet() {
            private static final long serialVersionUID = 1L;
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException
            {
                try
                {
                    resp.getOutputStream().print(TEST_MSG);
                }
                catch (IOException e)
                {
                    throw new ServletException(e);
                }
            }
        }),"/test");
    }
    
    
    public static HttpServer getInstance()
    {
        if (instance == null)
            instance = new HttpServer();
        
        return instance;
    }
    

    @Override
    public void updateConfig(HttpServerConfig config) throws SensorHubException
    {
        stop();
        init(config);
        start();
    }

    
    @Override
    public void start() throws SensorHubException
    {
        try
        {
            server = new Server(config.httpPort);
            
            HandlerList handlers = new HandlerList();
            
            if (config.docRoot != null)
            {
                ResourceHandler resourceHandler = new ResourceHandler();
                resourceHandler.setResourceBase(config.docRoot);
                handlers.addHandler(resourceHandler);
                log.info("Serving static resources from " + config.docRoot);
            }
            
            if (config.rootURL != null)
            {
                servletHandler.setContextPath(config.rootURL);
                handlers.addHandler(servletHandler);
                log.info("Serving servlets at " + config.rootURL);
            }
            
            server.setHandler(handlers);
            server.start();
            log.info("HTTP server started");
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error while starting SensorHub embedded HTTP server", e);
        }
    }
    
    
    @Override
    public void stop() throws SensorHubException
    {
        try
        {
            server.stop();
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error while stopping SensorHub embedded HTTP server", e);
        }
    }
    
    
    public void deployServlet(String path, HttpServlet servlet)
    {
        deployServlet(path, servlet, null);
    }
    
    
    public void deployServlet(String path, HttpServlet servlet, Map<String, String> initParams)
    {
        ServletHolder servletHolder = new ServletHolder(servlet);
        if (initParams != null)
            servletHolder.setInitParameters(initParams);
        servletHandler.addServlet(servletHolder, path);
    }
    
    
    public void undeployServlet(HttpServlet servlet)
    {
        servletHandler.removeBean(servlet);
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        stop();
        server = null;
    }
    
    
    public Server getJettyServer()
    {
        return server;
    }
}
