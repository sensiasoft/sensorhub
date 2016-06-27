/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.DatagramChannel;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Communication provider for UDP links
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 12, 2015
 */
public class UDPCommProvider extends AbstractModule<UDPCommProviderConfig> implements ICommProvider<UDPCommProviderConfig>
{
    static final Logger log = LoggerFactory.getLogger(UDPCommProvider.class.getSimpleName());
    
    DatagramChannel channel;
    InputStream is;
    OutputStream os;
    
    
    public UDPCommProvider() 
    {
    }
    
    
    @Override
    public InputStream getInputStream() throws IOException
    {
        return is;
    }


    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return os;
    }


    @Override
    public void start() throws SensorHubException
    {        
        UDPConfig config = this.config.protocol;
        
        try
        {
            SocketAddress localAddr = new InetSocketAddress(config.localPort);
            SocketAddress remoteAddr = new InetSocketAddress(config.remoteHost, config.remotePort);
            channel = DatagramChannel.open().bind(localAddr);
            
            channel.connect(remoteAddr);
            is = Channels.newInputStream(channel);
            os = Channels.newOutputStream(channel);
        }
        catch (IOException e)
        {
            throw new SensorHubException("Cannot setup UDP socket between local address " + config.localPort +
                                         " and remote host " + config.remoteHost + ":" + config.remotePort);
        }
    }


    @Override
    public void stop() throws SensorHubException
    {
        try
        {
            channel.close();
        }
        catch (IOException e)
        {
        }        
    }


    @Override
    public void cleanup() throws SensorHubException
    {        
    }
}
