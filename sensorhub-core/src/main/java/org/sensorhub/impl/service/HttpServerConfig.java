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

import java.util.List;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.module.ModuleConfig;


/**
 * <p>
 * Configuration class for the HTTP server module
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 6, 2013
 */
public class HttpServerConfig extends ModuleConfig
{
     
    @DisplayInfo(desc="TCP port where HTTP server will listen")
    public int httpPort = 8080;
    
    
    @DisplayInfo(desc="Root URL where static web content will be served.")
    public String staticDocRootUrl = null;
    
    
    @DisplayInfo(desc="Root URL where the server will accept requests. This will be the prefix to all servlet URLs.")
    public String servletsRootUrl = "/sensorhub";
    
    
    @DisplayInfo(desc="Maximum number of requests per second allowed per session/connection")
    public int maxRequestsPerSecond = 20;
    
    
    @DisplayInfo(desc="List of users with passwords and roles (format is user: password[, role1, role2 ...]")
    public List<String> users;
    

    public HttpServerConfig()
    {
        this.id = "HTTP_SERVER_0";
        this.name = "HTTP Server";
        this.moduleClass = HttpServer.class.getCanonicalName();
        this.autoStart = true;
    }
    
    
    public int getHttpPort()
    {
        return httpPort;
    }


    public void setHttpPort(int httpPort)
    {
        this.httpPort = httpPort;
    }


    public String getServletsRootUrl()
    {
        return servletsRootUrl;
    }


    public void setServletsRootUrl(String rootURL)
    {
        this.servletsRootUrl = rootURL;
    }
    
}
