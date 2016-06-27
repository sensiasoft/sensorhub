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
    

    public boolean tryConnect(String host) throws Exception
    {
        return tryConnect(host, -1);
    }
    
    
    public boolean tryConnect(String host, int port) throws Exception
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
                        module.reportError("Cannot ping host " + host, null);
                        return false;
                    }
                }
                else
                {
                    boolean reachable = IPNetworkUtils.isHostReachable(host, port, connectConfig.connectTimeout);
                    if (!reachable)
                    {
                        module.reportError("Cannot reach host " + host + " on port " + port, null);
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
            module.reportError("Cannot resolve hostname " + host, null);
            return false;
        }
        
        return true;
    }

}
