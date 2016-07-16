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

import java.io.IOException;
import java.net.UnknownHostException;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.RobustConnection;


/**
 * <p>
 * Helper class for handling automatic reconnections to hosts on IP networks
 * </p>
 *
 * @author Alex Robin
 * @since Jun 25, 2016
 */
public abstract class RobustIPConnection extends RobustConnection
{
    protected boolean checkReachability;
    
    
    public RobustIPConnection(AbstractModule<?> module, RobustIPConnectionConfig config, String remoteServiceName)
    {
        super(module, config, remoteServiceName);
        this.checkReachability = config.checkReachability;
    }
    

    /**
     * Try to ping host with ICMP packets (must be root)
     * @param host host name or address to connect to
     * @return True if connection was successful; False if unsuccessful but can be retried
     * @throws IOException if connection was unsuccessful and shouldn't be retried
     */
    public boolean tryConnect(String host) throws IOException
    {
        return tryConnectTCP(host, -1);
    }
    
    
    /**
     * Try to connect to host on given TCP port
     * @param host host name or address to connect to
     * @param port TCP port to connect to
     * @return True if connection was successful; False if unsuccessful but can be retried
     * @throws IOException if connection was unsuccessful and shouldn't be retried
     */
    public boolean tryConnectTCP(String host, int port) throws IOException
    {
        try
        {
            // if ping enabled, check that host is reachable
            if (checkReachability)
            {
                if (port < 0)
                {
                    boolean reachable = IPNetworkUtils.isHostReachable(host, connectConfig.connectTimeout);
                    if (!reachable)
                    {
                        module.reportError("Cannot ping host " + host, null, true);
                        return false;
                    }
                }
                else
                {
                    boolean reachable = IPNetworkUtils.isHostReachable(host, port, connectConfig.connectTimeout);
                    if (!reachable)
                    {
                        module.reportError("Cannot reach host " + host + " on port " + port, null, true);
                        return false;
                    }
                }                
            }
            
            // else just check that host name is resolvable
            else
                IPNetworkUtils.resolveHost(host, connectConfig.connectTimeout);
        }
        catch (UnknownHostException e)
        {
            module.reportError("Cannot resolve hostname " + host, null, true);
            return false;
        }
        
        return true;
    }

}
