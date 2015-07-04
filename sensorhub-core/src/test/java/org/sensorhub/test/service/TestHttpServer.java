/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service;

import static org.junit.Assert.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;


public class TestHttpServer
{

    @Test
    public void testStartServer() throws Exception
    {
        HttpServer server = HttpServer.getInstance();
        HttpServerConfig config = new HttpServerConfig();
        server.init(config);
        server.start();
        
        // connect to servlet and check response
        URL url = new URL("http://localhost:" + config.httpPort + config.servletsRootUrl + "/test");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String resp = reader.readLine();
        System.out.println(resp);
        reader.close();
        
        assertTrue(resp.equals(HttpServer.TEST_MSG));        
        server.stop();
    }
    
    
    @Test
    public void testDeployServlet() throws Exception
    {
        // start server
        HttpServer server = HttpServer.getInstance();
        HttpServerConfig config = new HttpServerConfig();
        server.init(config);
        server.start();
        
        final String testText = "Deploying hot servlet in SensorHub works";
        
        // deploy new servlet dynamically
        server.deployServlet(new HttpServlet() {
            private static final long serialVersionUID = 1L;
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException
            {
                try
                {
                    resp.getOutputStream().print(testText);
                    resp.getOutputStream().flush();
                }
                catch (IOException e)
                {
                    throw new ServletException(e);
                }
            }
        }, "/junit");
        
        // connect to servlet and check response
        URL url = new URL("http://localhost:" + config.httpPort + config.servletsRootUrl + "/junit");
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        String resp = reader.readLine();
        System.out.println(resp);
        reader.close();
        
        assertTrue(resp.equals(testText));
        server.stop();
    }
}
