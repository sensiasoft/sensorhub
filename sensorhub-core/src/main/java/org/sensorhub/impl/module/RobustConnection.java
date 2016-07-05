/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.module;

import org.sensorhub.api.client.ClientException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * Helper class for handling automatic reconnections to remote devices/services.<br/>
 * It must be subclassed to provide an implementation of the {@link #tryConnect()}
 * method 
 * </p>
 *
 * @author Alex Robin
 * @since Jun 25, 2016
 */
public abstract class RobustConnection
{   
    private final static int UPDATE_STATUS_PERIOD = 5000;
    
    protected AbstractModule<?> module;
    protected RobustConnectionConfig connectConfig;
    protected String remoteServiceName;
    protected Thread waitThread;
    protected volatile boolean connected;
    
    
    public RobustConnection(AbstractModule<?> module, RobustConnectionConfig config, String remoteServiceName)
    {
        this.module = module;
        this.connectConfig = config;
        this.remoteServiceName = remoteServiceName;
    }
    
    
    public void updateConfig(RobustConnectionConfig config)
    {
        this.connectConfig = config;
    }
    
    
    /**
     * Implements the connection process.<br/>
     * This is called for each connection attempt
     * @return true if successfully connected, false otherwise
     * @throws Exception sent on error that requires aborting the connection process 
     * (no more connection attempts will be made after such exception is thrown)
     */
    public abstract boolean tryConnect() throws Exception;
    
    
    /**
     * Waits until the client is connected<br/>
     * If connection is detected by another thread, this method can also be notified
     * by calling stateLock.notify()
     * @throws SensorHubException on error preventing further attempts or max number of attempts reached
     */
    public void waitForConnection() throws SensorHubException
    {
        synchronized (module.stateLock)
        {
            if (isConnected())
                return;
            
            waitThread = Thread.currentThread();
            
            try
            {
                int numAttempts = 0;
                while (!isConnected())
                {
                    String msg = String.format("Attempting to connect to %s (%d/%d)", remoteServiceName, numAttempts+1, connectConfig.reconnectAttempts+1);
                    module.reportStatus(msg);
                    
                    // try to connect
                    try
                    {
                        connected = tryConnect();
                    }
                    catch (Exception e)
                    {
                        throw new ClientException("Cannot connect to " + remoteServiceName, e);
                    }
                    
                    // abort if too many attempts
                    numAttempts++;
                    if (!isConnected() && numAttempts > connectConfig.reconnectAttempts)
                        throw new ClientException("Maximum number of connection attempts reached", module.getCurrentError());
                                        
                    // wait before trying again
                    long nextAttemptTime = System.currentTimeMillis() + connectConfig.reconnectPeriod;
                    while (!isConnected() && System.currentTimeMillis() < nextAttemptTime)
                    {
                        long waitTime = Math.max(0, nextAttemptTime - System.currentTimeMillis());
                        
                        // update status only after 5s
                        if (waitTime >= 800 && waitTime <= connectConfig.reconnectPeriod-5000)
                        {
                            msg = String.format("Will retry connection in %.0fs", waitTime/1000.);
                            module.reportStatus(msg);
                        }
                        
                        // wait at most 1s so we can update the status regularly
                        module.stateLock.wait(Math.min(waitTime, UPDATE_STATUS_PERIOD));
                    }
                }
                
                // clear error and set status message on successful connection
                module.clearError();
                module.notifyConnectionStatus(isConnected(), remoteServiceName);
            }
            catch (InterruptedException e)
            {
                connected = false;
                throw new SensorHubException("Automatic reconnection loop interrupted");
            }
            finally
            {
                waitThread = null;
            }
        }
    }
    
    
    public void cancel()
    {
        if (waitThread != null)
            waitThread.interrupt();
    }
    
    
    /**
     * Helper method to reconnect after a disconnection has been detected<br/>
     * The default implementation just restarts the module asynchronously.
     */
    public void reconnect()
    {
        connected = false;
        
        // send disconnection event
        module.notifyConnectionStatus(false, remoteServiceName);
        
        // restart in separate thread
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    module.requestStop();
                    module.requestStart();
                }
                catch (SensorHubException e)
                {
                    module.reportError("Error while reconnecting to " + remoteServiceName, e);
                }
            }
        }).start();
    }
    
    
    public boolean isConnected()
    {
        // default implementation just returns the value of the connected flag
        // this is good enough if the connect() method is synchronous
        return connected;
    }
}
