/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.client;

import org.sensorhub.api.client.ClientConfig;
import org.sensorhub.api.client.ClientException;
import org.sensorhub.api.client.IClientModule;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * Base client implementation providing helper method to handle automatic
 * reconnections
 * </p>
 *
 * @author Alex Robin
 * @param <ConfigType> Type of client configuration
 * @since Jun 16, 2016
 */
public abstract class AbstractClient<ConfigType extends ClientConfig> extends AbstractModule<ConfigType> implements IClientModule<ConfigType>
{   
    volatile boolean connected;
    
    
    /**
     * Implements the connection process.<br/>
     * This is called for each connection attempt
     * @return true if successfully connected, false otherwise
     * @throws SensorHubException on fatal error that require aborting the connection process 
     * (no more connection attempts will be made)
     */
    protected abstract boolean connect() throws SensorHubException;
    
    
    /**
     * Waits until the client is connected or timeout occurs.<br/>
     * If connection is detected by another thread, this method can also be notified
     * by calling stateLock.notify()
     * @throws SensorHubException
     */
    protected void waitForConnection() throws SensorHubException
    {
        synchronized (stateLock)
        {
            try
            {
                int numAttempts = 1;
                while (!isConnected())
                {
                    String msg = String.format("Attempting to connect to remote service (%d/%d)", numAttempts, config.reconnectAttempts);
                    reportStatus(msg);
                    
                    // compute time of next attempt
                    long nextAttemptTime = System.currentTimeMillis() + config.reconnectPeriod;
                    
                    // try to connect
                    try
                    {
                        connected = connect();
                    }
                    catch (Exception e)
                    {
                        throw new ClientException("Cannot connect to remote service", e);
                    }
                    
                    // abort if too many attempts
                    numAttempts++;
                    if (!isConnected() && numAttempts > config.reconnectAttempts)
                        throw new ClientException("Maximum number of connection attempts reached");
                    
                    // wait before trying again
                    while (!isConnected() && System.currentTimeMillis() < nextAttemptTime)
                    {
                        long waitTime = Math.max(0, nextAttemptTime - System.currentTimeMillis());
                        msg = String.format("Retrying connection in %.1f s", waitTime/1000.0);
                        reportStatus(msg);
                        
                        // wait at most 1s so we can update the status
                        stateLock.wait(Math.min(waitTime, 1000));
                    }
                }
                
                // clear messages on successful connection
                clearError();
                clearStatus();
            }
            catch (InterruptedException e)
            {
            }
        }
    }
    
    
    /**
     * Helper method to restart the client after a disconnection
     */
    public void restartOnDisconnect()
    {
        connected = false;
        
        // restart in separate thread
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    requestStop();
                    requestStart();
                }
                catch (SensorHubException e)
                {
                    reportError("Error while reconnecting to remote service", e);
                }
            }
        }).start();
    }
    
    
    @Override
    public boolean isConnected()
    {
        // default implementation just returns the value of the connected flag
        // this is good enough if the connect() method is synchronous
        return connected;
    }
}
