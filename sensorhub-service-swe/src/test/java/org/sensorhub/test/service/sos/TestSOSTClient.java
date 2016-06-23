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

import static org.junit.Assert.*;
import net.opengis.swe.v20.DataBlock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.client.ClientException;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.client.sos.SOSClient.SOSRecordListener;
import org.sensorhub.impl.client.sost.SOSTClient;
import org.sensorhub.impl.client.sost.SOSTClientConfig;
import org.sensorhub.impl.service.sos.SOSProviderConfig;
import org.sensorhub.test.sensor.FakeSensor;
import org.sensorhub.test.sensor.FakeSensorData;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSUtils;
import org.vast.ows.sos.GetResultRequest;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.ows.sos.SOSServiceCapabilities;
import org.vast.ows.sos.SOSUtils;
import org.vast.util.TimeExtent;


public class TestSOSTClient implements SOSRecordListener
{
    static final int TIMEOUT = 10000;
    static final String SENSOR_UID = "urn:test:newsensor:0002";
    static final String NAME_OUTPUT1 = "weatherData";
    static final double SAMPLING_PERIOD = 0.2;
    static final int NUM_GEN_SAMPLES = 4;
    TestSOSService sosTest;
    Exception connectError;
    int recordCounter = 0;
    
    
    @Before
    public void setup() throws Exception
    {
        sosTest = new TestSOSService();
        sosTest.setup();
    }
    
    
    protected ISensorModule<?> buildSensor1(boolean start) throws Exception
    {
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.autoStart = false;
        sensorCfg.moduleClass = FakeSensorNetWithFoi.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        FakeSensor sensor = (FakeSensor)sosTest.registry.loadModule(sensorCfg);
        sensor.setSensorUID(SENSOR_UID);
        sensor.setDataInterfaces(new FakeSensorData(sensor, NAME_OUTPUT1, 10, SAMPLING_PERIOD, NUM_GEN_SAMPLES));
        if (start)
            sensor.start();        
        return sensor;
    }
    
    
    protected void startClient(String sensorID, boolean async) throws Exception
    {
        startClient(sensorID, async, 10);
    }
    
    
    protected void startClient(String sensorID, boolean async, int maxAttempts) throws Exception
    {
        SOSTClientConfig config = new SOSTClientConfig();
        config.id = "SOST";
        config.name = "SOS-T Client";
        config.connectTimeout = 1000;
        config.reconnectPeriod = 500;
        config.reconnectAttempts = maxAttempts;
        config.sensorID = sensorID;
        config.sosEndpointUrl = TestSOSService.HTTP_ENDPOINT;
        
        final SOSTClient client = new SOSTClient();
        client.init(config);
        
        if (async)
        {
            new Thread() {
                public void run()
                {
                    try
                    {
                        client.start();
                    }
                    catch (SensorHubException e)
                    {
                        connectError = e;
                    }
                }
            }.start();
        }
        else
            client.start();
    }
    
    
    protected SOSOfferingCapabilities getCapabilities(int offeringIndex, int waitTimeOut) throws Exception
    {
        OWSUtils utils = new OWSUtils();
        
        // check capabilities has one more offering
        GetCapabilitiesRequest getCap = new GetCapabilitiesRequest();
        getCap.setService(SOSUtils.SOS);
        getCap.setVersion("2.0");
        getCap.setGetServer(TestSOSService.HTTP_ENDPOINT);
        
        long maxWait = System.currentTimeMillis() + waitTimeOut;
        SOSServiceCapabilities caps = null;
        int numOffering = 0;
        do
        {
            caps = (SOSServiceCapabilities)utils.sendRequest(getCap, false);
            numOffering = caps.getLayers().size();
            if (numOffering >= offeringIndex+1)
                break;
            if (waitTimeOut > 0)
                Thread.sleep(1000);
        }
        while (System.currentTimeMillis() < maxWait);
        
        //utils.writeXMLResponse(System.out, caps);
        assertTrue("No offering added", numOffering >= offeringIndex+1);
        return (SOSOfferingCapabilities)caps.getLayers().get(offeringIndex);
    }
    
    
    @Test
    public void testRegisterSync() throws Exception
    {
        // start service with SOS-T support
        sosTest.deployService(true, new SOSProviderConfig[0]);
     
        // start client
        ISensorModule<?> sensor = buildSensor1(false);
        startClient(sensor.getLocalID(), false);
        
        // check capabilities content
        SOSOfferingCapabilities newOffering = getCapabilities(0, 0);
        assertEquals(SENSOR_UID, newOffering.getMainProcedure());
    }
    
    
    @Test(expected = ClientException.class)
    public void testRegisterErrorNoTransactionalServer() throws Exception
    {
        // start service w/o SOS-T support
        sosTest.deployService(false, new SOSProviderConfig[0]);
        
        // start client
        ISensorModule<?> sensor = buildSensor1(false);
        startClient(sensor.getLocalID(), false);
    }
    
    
    @Test
    public void testRegisterAsyncReconnect() throws Exception
    {
        // start client
        ISensorModule<?> sensor = buildSensor1(false);
        startClient(sensor.getLocalID(), true);
        Thread.sleep(100);
        
        // start service
        sosTest.deployService(true, new SOSProviderConfig[0]);
        
        // check capabilities content
        SOSOfferingCapabilities newOffering = getCapabilities(0, TIMEOUT);
        assertEquals(SENSOR_UID, newOffering.getMainProcedure());
    }
    
    
    @Test(expected = ClientException.class)
    public void testRegisterAsyncReconnectNoServer() throws Exception
    {
        // start client
        ISensorModule<?> sensor = buildSensor1(false);
        startClient(sensor.getLocalID(), true, 3);
        
        // wait for exception
        long maxWait = System.currentTimeMillis() + TIMEOUT;
        while (connectError == null)
        {
            Thread.sleep(500);
            if (System.currentTimeMillis() > maxWait)
                fail("No connection error reported");
        }
        
        throw connectError;
    }
    
    
    @Test
    public void testInsertResult() throws Exception
    {
        GetResultRequest req = new GetResultRequest();
        req.setGetServer(TestSOSService.HTTP_ENDPOINT);
        req.setVersion("2.0");
        req.setOffering(SENSOR_UID + "-sos");
        req.getObservables().add(TestSOSService.URI_PROP1);
        req.setTime(TimeExtent.getPeriodStartingNow((System.currentTimeMillis()+60000) / 1000.));
        req.setXmlWrapper(false);
        
        
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
