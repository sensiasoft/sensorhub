/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.comm;

import static org.junit.Assert.*;
import java.net.UnknownHostException;
import org.junit.Test;
import org.sensorhub.impl.comm.IPNetworkUtils;


public class TestIpNetworkUtils
{
    
    @Test
    public void testResolveValidHost() throws Exception
    {
        int timeOut = 10000;
        
        long t0 = System.currentTimeMillis();
        IPNetworkUtils.resolveHost("www.google.com", timeOut);
        long t1 = System.currentTimeMillis();
        
        assertTrue("DNS resolution timeout expired", t1-t0 < timeOut);
    }    
    
    
    @Test
    public void testResolveInvalidHost() throws Exception
    {
        int timeOut = 1000;
        
        try
        {
            long t0 = System.currentTimeMillis();
            IPNetworkUtils.resolveHost("fdazdaztger", timeOut);
            long t1 = System.currentTimeMillis();
            
            assertTrue("DNS resolution timeout should have expired", t1-t0 >= timeOut);
        }
        catch (UnknownHostException e)
        {
            return;
        }
        catch (Exception e)
        {
            fail("Unexpected Exception: " + e.getMessage());
        }
        
        fail("Host should not be reachable");
    }
    
    
    @Test
    public void testPingLocalHost() throws Exception
    {
        int timeOut = 10000;
        
        long t0 = System.currentTimeMillis();
        boolean reachable = IPNetworkUtils.isHostReachable("localhost", timeOut);
        long t1 = System.currentTimeMillis();
        
        assertTrue("Timeout expired", t1-t0 < timeOut);
        assertTrue("Localhost is not reachable", reachable);
    } 
    
    
    @Test
    public void testPingInvalidHost() throws Exception
    {
        int timeOut = 100;
        boolean reachable = IPNetworkUtils.isHostReachable("172.1.1.1", timeOut);
        assertFalse("Invalid host should not be reachable", reachable);
    }

}
