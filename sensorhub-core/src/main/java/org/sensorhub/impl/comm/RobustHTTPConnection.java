/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * Helper class for handling automatic reconnections to HTTP services
 * </p>
 *
 * @author Alex Robin
 * @since Jun 29, 2016
 */
public abstract class RobustHTTPConnection extends RobustIPConnection
{
    
    
    public RobustHTTPConnection(AbstractModule<?> module, RobustIPConnectionConfig config, String remoteServiceName)
    {
        super(module, config, remoteServiceName);
    }
    
    
    private HttpURLConnection tryConnectHTTP(String url, String postData) throws IOException
    {
        URL urlObj = new URL(url);
        
        // try to connect to URL
        HttpURLConnection conn = null;
        try
        {
            conn = (HttpURLConnection)urlObj.openConnection();
            conn.setConnectTimeout(connectConfig.connectTimeout);
            conn.setReadTimeout(connectConfig.connectTimeout);
            if (postData != null)
            {
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
            }
            conn.connect();            
        }
        catch (IOException e)
        {
            // test DNS and TCP connection to get more precise error message
            int port = urlObj.getPort();
            tryConnectTCP(urlObj.getHost(), (port < 0) ? 80 : port);
            
            module.reportError("Cannot connect to HTTP server", e, true);
            return null;
        }
        
        if (postData != null)
        {
            // send POST data
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            out.write(postData);
            out.close();
        }
        
        // check response error code
        int errorCode = conn.getResponseCode();
        if (errorCode < 0)
            throw new IOException("Received invalid HTTP response");
        else if (errorCode >= 400)
            throw new IOException("Received HTTP error code " + conn.getResponseCode());
        
        return conn;
    }
    

    /**
     * Try to connect to URL using GET method
     * @param url URL to connect to
     * @return Connection object or null if connection was unsuccessful but can be retried
     * @throws IOException if connection was unsuccessful and shouldn't be retried
     */
    public HttpURLConnection tryConnectGET(String url) throws IOException
    {
        return tryConnectHTTP(url, null);        
    }
    
    
    /**
     * Try to connect to URL and send POST data
     * @param url URL to connect to
     * @param postData data to send as POST
     * @return Connection object or null if connection was unsuccessful but can be retried
     * @throws IOException if connection was unsuccessful and shouldn't be retried
     */
    public HttpURLConnection tryConnectPOST(String url, String postData) throws IOException
    {
        return tryConnectHTTP(url, postData);
    }
}
