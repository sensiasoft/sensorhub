/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sos;

import static org.junit.Assert.assertTrue;
import net.opengis.swe.v20.DataBlock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.impl.client.sos.SOSClient;
import org.sensorhub.impl.client.sos.SOSClient.SOSRecordListener;
import org.vast.ows.sos.GetResultRequest;
import org.vast.util.TimeExtent;


public class TestSOSClient implements SOSRecordListener
{
    TestSOSService sosTest;
    int recordCounter = 0;
    
    
    @Before
    public void setup() throws Exception
    {
        sosTest = new TestSOSService();
        sosTest.setup();
    }
    
    
    @Test
    public void testConnectHttp() throws Exception
    {
        GetResultRequest req = new GetResultRequest();
        req.setGetServer(TestSOSService.HTTP_ENDPOINT);
        req.setVersion("2.0");
        req.setOffering(TestSOSService.URI_OFFERING1);
        req.getObservables().add(TestSOSService.URI_PROP1);
        req.setTime(TimeExtent.getPeriodStartingNow((System.currentTimeMillis()+60000) / 1000.));
        req.setXmlWrapper(false);
        
        SOSClient client = new SOSClient(req, false);
        
        // start service and client
        sosTest.testSetupService();
        client.retrieveStreamDescription();
        client.startStream(this);
        
        // wait until some records have been received
        Thread.sleep(1000L);
        assertTrue(recordCounter > 0);
    }
    
    
    @Test
    public void testConnectWebsockets() throws Exception
    {
        GetResultRequest req = new GetResultRequest();
        req.setGetServer(TestSOSService.HTTP_ENDPOINT);
        req.setVersion("2.0");
        req.setOffering(TestSOSService.URI_OFFERING1);
        req.getObservables().add(TestSOSService.URI_PROP1);
        req.setTime(TimeExtent.getPeriodStartingNow((System.currentTimeMillis()+60000) / 1000.));
        req.setXmlWrapper(false);
        
        SOSClient client = new SOSClient(req, true);
        
        // start service and client
        sosTest.testSetupService();
        client.retrieveStreamDescription();
        client.startStream(this);

        // wait until some records have been received
        Thread.sleep(1000L);
        assertTrue(recordCounter > 0);
    }
    
    
    @Override
    public void newRecord(DataBlock data)
    {
        System.out.println("Record received: " + data);
        recordCounter++;
    }    
    
   
    @After
    public void cleanup()
    {
        sosTest.cleanup();
    }
}
