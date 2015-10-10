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

import static org.junit.Assert.assertEquals;
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
    TestSOSService sos;
    int recordCounter = 0;
    
    
    @Before
    public void setup() throws Exception
    {
        sos = new TestSOSService();
        sos.setupFramework();
    }
    
    
    @Test
    public void testConnectHttp() throws Exception
    {
        GetResultRequest req = new GetResultRequest();
        req.setGetServer(TestSOSService.SERVICE_ENDPOINT);
        req.setVersion("2.0");
        req.setOffering(TestSOSService.URI_OFFERING1);
        req.getObservables().add(TestSOSService.URI_PROP1);
        req.setTime(TimeExtent.getPeriodStartingNow((System.currentTimeMillis()+60000) / 1000.));
        req.setXmlWrapper(false);
        
        SOSClient client = new SOSClient(req, false);
        
        // start service and client
        sos.testSetupService();
        client.retrieveStreamDescription();
        client.startStream(this);
        
        // wait until all records have been received or timeout
        long t0 = System.currentTimeMillis();
        while (recordCounter < TestSOSService.NUM_GEN_SAMPLES && System.currentTimeMillis() - t0 < 5000)
            Thread.sleep(500L);        
        
        assertEquals(TestSOSService.NUM_GEN_SAMPLES, recordCounter);
    }
    
    
    @Test
    public void testConnectWebsockets() throws Exception
    {
        GetResultRequest req = new GetResultRequest();
        req.setGetServer(TestSOSService.SERVICE_ENDPOINT);
        req.setVersion("2.0");
        req.setOffering(TestSOSService.URI_OFFERING1);
        req.getObservables().add(TestSOSService.URI_PROP1);
        req.setTime(TimeExtent.getPeriodStartingNow((System.currentTimeMillis()+60000) / 1000.));
        req.setXmlWrapper(false);
        
        SOSClient client = new SOSClient(req, true);
        
        // start service and client
        sos.testSetupService();
        client.retrieveStreamDescription();
        client.startStream(this);

        // wait until all records have been received or timeout
        long t0 = System.currentTimeMillis();
        while (recordCounter < TestSOSService.NUM_GEN_SAMPLES && System.currentTimeMillis() - t0 < 5000)
            Thread.sleep(500L);        
        
        assertEquals(TestSOSService.NUM_GEN_SAMPLES, recordCounter);
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
        sos.cleanup();
    }
}
