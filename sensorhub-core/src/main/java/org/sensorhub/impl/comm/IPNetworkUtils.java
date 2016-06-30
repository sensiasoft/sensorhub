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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * <p>
 * Static helper methods for network operations
 * </p>
 *
 * @author Alex Robin
 * @since Jun 11, 2016
 */
public class IPNetworkUtils
{
    
    /**
     * @return true if at least one network interface is up
     */
    public static boolean isNetworkUp()
    {
        // retrieve list of network interfaces
        Enumeration<NetworkInterface> nets;
        try
        {
            nets = NetworkInterface.getNetworkInterfaces();
        }
        catch (SocketException e)
        {
            throw new RuntimeException(e);
        }
        
        // check if at least one interface is up
        for (NetworkInterface iface : Collections.list(nets))
        {
            try
            {
                if (iface.isLoopback())
                    continue;
                
                if (iface.isUp())
                    return true;
            }
            catch (SocketException e)
            {
            }
        }
        
        return false;
    }
    
    
    /**
     * Resolves hostname to its IP address(s) unless the time out
     * @param host Host name to resolve
     * @param timeOut Timeout duration in milliseconds
     * @return the list of resolved IP adresses for the given host name
     * @throws UnknownHostException if no address are known for this host
     */
    public static InetAddress[] resolveAll(final String host, final int timeOut) throws UnknownHostException
    {
        if (host == null || host.trim().isEmpty())
            throw new IllegalArgumentException("Host cannot be null");
        
        // launch hostname resolution in separate thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<InetAddress[]> result = executor.submit(new Callable<InetAddress[]>()
        {
            @Override
            public InetAddress[] call() throws UnknownHostException
            {
                return InetAddress.getAllByName(host);
            }
        });

        try
        {
            return result.get(timeOut, TimeUnit.MILLISECONDS);
        }
        catch (ExecutionException e)
        {
            if (e.getCause() instanceof UnknownHostException)
                throw (UnknownHostException)e.getCause();
            else
                throw new RuntimeException(e);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
        catch (TimeoutException e)
        {
            throw new UnknownHostException("DNS timeout while resolving hostname " + host);
        }
    }
    
    
    /**
     * Resolves hostname to its IP address unless the timeout delay is reached
     * @param host Host name to resolve
     * @param timeOut Timeout duration in milliseconds
     * @return the IP address of the given host
     * @throws UnknownHostException
     */
    public static InetAddress resolveHost(final String host, final int timeOut) throws UnknownHostException
    {
        return resolveAll(host, timeOut)[0];
    }
    
    
    /**
     * Resolves hostname and check if accessible with {@link InetAddress#isReachable(int)}
     * @param host Host name to resolve and test for reachability
     * @param timeOut Timeout duration in milliseconds
     * @return true if host is reachable
     * @throws UnknownHostException
     * @throws IOException
     */
    public static boolean isHostReachable(final String host, final int timeOut) throws UnknownHostException, IOException
    {
        long t0 = System.currentTimeMillis();
        InetAddress ip = resolveHost(host, timeOut);
        int ellapsed = (int)(System.currentTimeMillis() - t0); 
                
        return ip.isReachable(Math.max(0, timeOut - ellapsed));
    }
    
    
    /**
     * Resolves hostname and check if accessible on specified port
     * @param host Host name to resolve and test for reachability
     * @param port TCP port to connect to
     * @param timeOut Timeout duration in milliseconds
     * @return true if host is reachable on this port
     * @throws UnknownHostException
     * @throws IOException
     */
    public static boolean isHostReachable(final String host, final int port, final int timeOut) throws UnknownHostException, IOException
    {
        long t0 = System.currentTimeMillis();
        InetAddress ip = resolveHost(host, timeOut);
        int ellapsed = (int)(System.currentTimeMillis() - t0); 
                
        try (Socket soc = new Socket())
        {
            soc.connect(new InetSocketAddress(ip, port), timeOut - ellapsed);
        }
        catch (IOException ex)
        {
            return false;
        }
        
        return true;
    }

}
