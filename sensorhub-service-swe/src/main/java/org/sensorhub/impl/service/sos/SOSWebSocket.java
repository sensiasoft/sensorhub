/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.io.EOFException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ows.OWSRequest;
import org.vast.ows.sos.GetResultRequest;


/**
 * <p>
 * Output only websocket for sending SOS live responses
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 19, 2015
 */
public class SOSWebSocket implements WebSocketCreator, WebSocketListener, Runnable
{
    private static final Logger log = LoggerFactory.getLogger(SOSWebSocket.class);
    
    Session session;
    SOSServlet parentService;
    OWSRequest request;
    WebSocketOutputStream respOutputStream;
    Executor threadPool;
    
    
    public SOSWebSocket(SOSServlet parentService, OWSRequest request)//, Executor threadPool)
    {
        this.parentService = parentService;
        this.request = request;
        this.threadPool = Executors.newSingleThreadExecutor();
        
        // enforce no XML wrapper to GetResult response
        if (request instanceof GetResultRequest)
            ((GetResultRequest) request).setXmlWrapper(false);
    }
    
    
    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp)
    {
        // return null if request was not accepted
        if (request == null)
            return null;
        
        return this;
    }
    
    
    @Override
    public void onWebSocketConnect(Session session)
    {
        this.session = session;
        
        respOutputStream = new WebSocketOutputStream(session, 1024);
        request.setResponseStream(respOutputStream);
        
        // launch processing in separate thread
        threadPool.execute(this);
    }
    
    
    @Override
    public void onWebSocketBinary(byte payload[], int offset, int len)
    {
    }


    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        session = null;
        respOutputStream.close();
        log.debug("Session closed");
    }
    
    
    @Override
    public void onWebSocketError(Throwable e)
    {
    }


    @Override
    public void onWebSocketText(String msg)
    {        
    }


    @Override
    public void run()
    {
        try
        {
            parentService.handleRequest(request);
            
            log.debug("Data provider done");
            if (session != null)
                session.close(StatusCode.NORMAL, null);
        }
        catch (EOFException e)
        {
            // if connection was closed by client during processing, we end on
            // an EOFException when writing next record to respOutputStream.
            // this is a normal state so we have nothing special to do
            log.debug("Data provider exited on client abort");
        }
        catch (Exception e)
        {
            log.debug("Data provider exited on error", e);
            if (session != null)
                session.close(StatusCode.PROTOCOL, e.getMessage());
        }
    }
}
