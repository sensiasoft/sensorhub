/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
public class NetworkUtils
{

    /**
     * Resolves hostname to its IP address unless the timeout delay is reached
     * @param host Host name to resolve
     * @param timeOut Timeout duration in milliseconds
     * @return the IP address of the given host
     * @throws UnknownHostException
     */
    public static InetAddress resolve(final String host, final long timeOut) throws UnknownHostException
    {
        return resolveAll(host, timeOut)[0];
    }


    /**
     * Resolves hostname to its IP address(s) unless the time out
     * @param host Host name to resolve
     * @param timeOut Timeout duration in milliseconds
     * @return the list of resolved IP adresses for the given host name
     * @throws UnknownHostException if no address are known for this host
     */
    public static InetAddress[] resolveAll(final String host, final long timeOut) throws UnknownHostException
    {
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
            throw (UnknownHostException)e.getCause();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return null;
        }
        catch (TimeoutException e)
        {
            throw new UnknownHostException("DNS timeout while resolving network address of host " + host);
        }
    }

}
