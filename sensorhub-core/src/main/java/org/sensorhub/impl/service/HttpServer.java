/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.rewrite.handler.HeaderPatternRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
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
            
            // static content
            if (config.staticDocRootUrl != null)
            {
                ResourceHandler resourceHandler = new ResourceHandler();
                resourceHandler.setResourceBase(config.staticDocRootUrl);
                handlers.addHandler(resourceHandler);
                log.info("Static resources root is " + config.staticDocRootUrl);
            }
            
            // servlets
            if (config.servletsRootUrl != null)
            {
                // authorize cross domain requests
                RewriteHandler rewrite = new RewriteHandler();
                HeaderPatternRule rule = new HeaderPatternRule();
                rule.setAdd(true);
                rule.setPattern("*");//config.servletsRootUrl + "/*");
                rule.setName("Access-Control-Allow-Origin");
                rule.setValue("*");
                rewrite.addRule(rule);
                handlers.addHandler(rewrite);
                
                servletHandler.setContextPath(config.servletsRootUrl);
                handlers.addHandler(servletHandler);
                log.info("Servlets root is " + config.servletsRootUrl);
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
        instance = null;
    }
    
    
    public Server getJettyServer()
    {
        return server;
    }
}
