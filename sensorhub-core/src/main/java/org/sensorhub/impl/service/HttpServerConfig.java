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

import org.sensorhub.api.module.ModuleConfig;


/**
 * <p>
 * Configuration class for the HTTP server module
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 6, 2013
 */
public class HttpServerConfig extends ModuleConfig
{
    private static final long serialVersionUID = -3737530047358507543L;

    
    /**
     * TCP port where HTTP server will listen
     */
    public int httpPort = 8080;
    
    
    /**
     * Root URL where the server will accept requests
     * This will be the prefix to all servlet URLs
     */
    public String servletsRootUrl = "/sensorhub";
    
    
    /**
     * Root folder where web documents will be obtained
     */
    public String staticDocRootUrl = null;
    


    public HttpServerConfig()
    {
        this.id = "???";
        this.name = "HTTP Server";
        this.moduleClass = HttpServer.class.getCanonicalName();
        this.enabled = true;
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
