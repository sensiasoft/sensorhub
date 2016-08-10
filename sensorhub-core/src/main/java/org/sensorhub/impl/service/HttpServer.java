/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlets.DoSFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Wrapper module for the HTTP server engine (Jetty for now)
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 6, 2013
 */
public class HttpServer extends AbstractModule<HttpServerConfig>
{
    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);
    public static String TEST_MSG = "SensorHub web server is up";
    private static HttpServer instance;
        
    Server server;
    ServletContextHandler servletHandler;
    ConstraintSecurityHandler securityHandler;
    HashLoginService loginService;
    
    
    public HttpServer()
    {
        if (instance != null)
            throw new RuntimeException("Cannot start several HTTP server instances");
        
        instance = this;
    }
    
    
    public static HttpServer getInstance()
    {
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
            
            // load user list
            loginService = new HashLoginService();
            loginService.setName("Authentication Required");
            loadUsers();
            
            // static content
            if (config.staticDocRootUrl != null)
            {
                ResourceHandler fileResourceHandler = new ResourceHandler();
                fileResourceHandler.setEtags(true);
                
                ContextHandler fileResourceContext = new ContextHandler();
                fileResourceContext.setContextPath("/");
                //fileResourceContext.setAllowNullPathInfo(true);
                fileResourceContext.setHandler(fileResourceHandler);
                fileResourceContext.setResourceBase(config.staticDocRootUrl);

                //fileResourceContext.clearAliasChecks();
                fileResourceContext.addAliasCheck(new AllowSymLinkAliasChecker());
                
                handlers.addHandler(fileResourceContext);
                log.info("Static resources root is " + config.staticDocRootUrl);
            }
            
            // servlets
            if (config.servletsRootUrl != null)
            {
                // create servlet handler
                this.servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);                
                servletHandler.setContextPath(config.servletsRootUrl);
                handlers.addHandler(servletHandler);
                log.info("Servlets root is " + config.servletsRootUrl);
                
                // DOS filter
                FilterHolder holder = servletHandler.addFilter(DoSFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
                holder.setInitParameter("maxRequestsPerSec", Integer.toString(config.maxRequestsPerSecond));
                holder.setInitParameter("remotePort", "true");
                holder.setInitParameter("insertHeaders", "false");
                holder.setInitParameter("maxRequestMs", Long.toString(24*3600*1000L)); // we need persistent requests!
                
                // security handler
                if (config.users != null && !config.users.isEmpty())
                {
                    securityHandler = new ConstraintSecurityHandler();
                    securityHandler.setAuthenticator(new DigestAuthenticator());
                    //securityHandler.setAuthenticator((Authenticator)Class.forName("org.sensorhub.impl.security.oauth.OAuthAuthenticator").newInstance());
                    securityHandler.setLoginService(loginService);
                    servletHandler.setSecurityHandler(securityHandler);
                }
                
                // filter to add proper cross-origin headers
                servletHandler.addFilter(CrossOriginFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
                
                // add default test servlet
                servletHandler.addServlet(new ServletHolder(new HttpServlet() {
                    private static final long serialVersionUID = 1L;
                    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException
                    {
                        try
                        {
                            log.debug("user = " + req.getRemoteUser() + ", admin = " + req.isUserInRole("admin"));
                            resp.getOutputStream().print(TEST_MSG);
                        }
                        catch (IOException e)
                        {
                            throw new ServletException(e);
                        }
                    }
                }),"/test");
            }
            
            server.setHandler(handlers);
            server.start();
            log.info("HTTP server started on port " + config.httpPort);
            
            setState(ModuleState.STARTED);
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error while starting SensorHub embedded HTTP server", e);
        }
    }
    
    
    private void loadUsers() throws ParseException
    {
        if (config.users != null)
        {            
            for (String userSpec: config.users)
            {
                String[] tokens = userSpec.split(":|,");
                if (tokens.length < 2)
                    throw new ParseException("Invalid user spec: " + userSpec, 0);
                String username = tokens[0].trim();
                String password = tokens[1].trim();
                String[] roles = new String[tokens.length-2];
                for (int i = 0; i < roles.length; i++)
                    roles[i] = tokens[i+2].trim();
                loginService.putUser(username, Credential.getCredential(password), roles);
            }
        }
    }
    
    
    @Override
    public void stop() throws SensorHubException
    {
        try
        {
            if (server != null)
            {
                server.stop();
                servletHandler = null;
            }
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error while stopping SensorHub embedded HTTP server", e);
        }
    }
    
    
    protected void checkStarted()
    {
        if (!isStarted())
            throw new RuntimeException("HTTP service must be started before servlets can be deployed");
    }
    
    
    public void deployServlet(HttpServlet servlet, String path)
    {
        deployServlet(servlet, null, path);
    }
    
    
    public synchronized void deployServlet(HttpServlet servlet, Map<String, String> initParams, String... paths)
    {
        checkStarted();
        
        ServletHolder holder = new ServletHolder(servlet);
        if (initParams != null)
            holder.setInitParameters(initParams);
        
        ServletMapping mapping = new ServletMapping();
        mapping.setServletName(holder.getName());
        mapping.setPathSpecs(paths);
        
        servletHandler.getServletHandler().addServlet(holder);
        servletHandler.getServletHandler().addServletMapping(mapping);
        log.debug("Servlet deployed " + mapping.toString());
    }
    
    
    public synchronized void undeployServlet(HttpServlet servlet)
    {
        // silently do nothing if server has already been shutdown
        if (servletHandler == null)
            return;
        
        try
        {
            // there is no removeServlet method so we need to do it manually
            ServletHandler handler = servletHandler.getServletHandler();
            
            // first collect servlets we want to keep
            List<ServletHolder> servlets = new ArrayList<ServletHolder>();
            String nameToRemove = null;
            for( ServletHolder holder : handler.getServlets() )
            {
                if (holder.getServlet() != servlet)
                    servlets.add(holder);
                else
                    nameToRemove = holder.getName();
            }

            if (nameToRemove == null)
                return;
            
            // also update servlet path mappings
            List<ServletMapping> mappings = new ArrayList<ServletMapping>();
            for (ServletMapping mapping : handler.getServletMappings())
            {
                if (!nameToRemove.contains(mapping.getServletName()))
                    mappings.add(mapping);
            }

            // set the new configuration
            handler.setServletMappings( mappings.toArray(new ServletMapping[0]) );
            handler.setServlets( servlets.toArray(new ServletHolder[0]) );
        }
        catch (ServletException e)
        {
            log.error("Error while undeploying servlet", e);
        }       
    }
    
    
    public void addServletSecurity(String pathSpec, String... roles)
    {
        checkStarted();
        
        if (securityHandler != null)
        {
            Constraint constraint = new Constraint();
            constraint.setName(Constraint.__DIGEST_AUTH);
            constraint.setRoles(roles);
            constraint.setAuthenticate(true);         
            ConstraintMapping cm = new ConstraintMapping();
            cm.setConstraint(constraint);
            cm.setPathSpec(pathSpec);
            securityHandler.addConstraintMapping(cm);
        }
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        server = null;
        instance = null;
    }
    
    
    public Server getJettyServer()
    {
        return server;
    }
}
