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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.persistence.IRecordStorageModule;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.persistence.InMemoryBasicStorage;
import org.sensorhub.impl.persistence.StreamStorageConfig;
import org.sensorhub.impl.persistence.perst.BasicStorageConfig;
import org.sensorhub.impl.persistence.perst.ObsStorageImpl;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.impl.service.ogc.OGCServiceConfig.CapabilitiesInfo;
import org.sensorhub.impl.service.sos.SOSProviderConfig;
import org.sensorhub.impl.service.sos.SOSService;
import org.sensorhub.impl.service.sos.SOSServiceConfig;
import org.sensorhub.impl.service.sos.SensorDataProviderConfig;
import org.sensorhub.test.sensor.FakeSensor;
import org.sensorhub.test.sensor.FakeSensorData;
import org.vast.data.DataBlockDouble;
import org.vast.data.QuantityImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.ogc.OGCException;
import org.vast.ogc.OGCExceptionReader;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSException;
import org.vast.ows.OWSExceptionReader;
import org.vast.ows.OWSRequest;
import org.vast.ows.OWSUtils;
import org.vast.ows.sos.GetFeatureOfInterestRequest;
import org.vast.ows.sos.GetObservationRequest;
import org.vast.ows.sos.InsertResultRequest;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.ows.sos.SOSServiceCapabilities;
import org.vast.ows.swe.DescribeSensorRequest;
import org.vast.swe.SWEData;
import org.vast.util.Bbox;
import org.vast.util.DateTimeFormat;
import org.vast.util.TimeExtent;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class TestSOSService
{
    static final long TIMEOUT = 10000;
    static final String NAME_OUTPUT1 = "weatherOut";
    static final String NAME_OUTPUT2 = "imageOut";
    static final String UID_SENSOR1 = "urn:sensors:mysensor:001";
    static final String UID_SENSOR2 = "urn:sensors:mysensor:002";
    static final String URI_OFFERING1 = "urn:mysos:sensor1";
    static final String URI_OFFERING2 = "urn:mysos:sensor2";
    static final String URI_PROP1 = "urn:blabla:weatherData";
    static final String URI_PROP1_FIELD1 = "urn:blabla:temperature";
    static final String URI_PROP1_FIELD2 = "urn:blabla:pressure";
    static final String URI_PROP2 = "urn:blabla:image";
    static final String NAME_OFFERING1 = "SOS Sensor Provider #1";
    static final String NAME_OFFERING2 = "SOS Sensor Provider #2";
    static final double SAMPLING_PERIOD = 0.1;
    static final int NUM_GEN_SAMPLES = 5;
    static final int NUM_GEN_FEATURES = 3;
    static final int SERVER_PORT = 8888;
    static final String SERVICE_PATH = "/sos";
    static final String HTTP_ENDPOINT = "http://localhost:" + SERVER_PORT + "/sensorhub" + SERVICE_PATH;
    static final String WS_ENDPOINT = HTTP_ENDPOINT.replace("http://", "ws://"); 
    static final String GETCAPS_REQUEST = "?service=SOS&version=2.0&request=GetCapabilities";
    static final String OFFERING_NODES = "contents/Contents/offering/*";
    static final String TIMERANGE_FUTURE = "now/2080-01-01";
    static final String TIMERANGE_NOW = "now";
    
    
    Map<Integer, Integer> obsFoiMap = new HashMap<Integer, Integer>();
    ModuleRegistry registry;
    File dbFile;
    
    
    @Before
    public void setup() throws Exception
    {
        // use temp DB file
        dbFile = File.createTempFile("osh-db-", ".dat");//new File(DB_PATH);
        dbFile.deleteOnExit();
        
        // get instance with in-memory DB
        registry = SensorHub.getInstance().getModuleRegistry();
        
        // start HTTP server
        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.httpPort = SERVER_PORT;
        registry.loadModule(httpConfig, TIMEOUT);
    }
    
    
    protected SOSService deployService(SOSProviderConfig... providerConfigs) throws Exception
    {
        return deployService(false, providerConfigs);
    }
    
    
    protected SOSService deployService(boolean enableSOST, SOSProviderConfig... providerConfigs) throws Exception
    {
        // create service config
        SOSServiceConfig serviceCfg = new SOSServiceConfig();
        serviceCfg.moduleClass = SOSService.class.getCanonicalName();
        serviceCfg.endPoint = SERVICE_PATH;
        serviceCfg.autoStart = true;
        serviceCfg.name = "SOS";
        serviceCfg.enableTransactional = enableSOST;
        CapabilitiesInfo srvcMetadata = serviceCfg.ogcCapabilitiesInfo;
        srvcMetadata.title = "My SOS Service";
        srvcMetadata.description = "An SOS service automatically deployed by SensorHub";
        srvcMetadata.serviceProvider.setOrganizationName("Test Provider, Inc.");
        srvcMetadata.serviceProvider.setDeliveryPoint("15 MyStreet");
        srvcMetadata.serviceProvider.setCity("MyCity");
        srvcMetadata.serviceProvider.setCountry("MyCountry");
        serviceCfg.dataProviders.addAll(Arrays.asList(providerConfigs));
        srvcMetadata.fees = "NONE";
        srvcMetadata.accessConstraints = "NONE";
        
        // start module
        SOSService sos = (SOSService)registry.loadModule(serviceCfg, TIMEOUT);
        
        // save config
        registry.saveModulesConfiguration();
        
        return sos;
    }
    
    
    protected SensorDataProviderConfig buildSensorProvider1() throws Exception
    {
        return buildSensorProvider1(true);
    }
    
    
    protected SensorDataProviderConfig buildSensorProvider1(boolean start) throws Exception
    {
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.autoStart = false;
        sensorCfg.moduleClass = FakeSensorNetWithFoi.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getModuleRegistry().loadModule(sensorCfg);
        sensor.setSensorUID(UID_SENSOR1);
        sensor.setDataInterfaces(new FakeSensorData(sensor, NAME_OUTPUT1, 10, SAMPLING_PERIOD, NUM_GEN_SAMPLES));
        if (start)
            SensorHub.getInstance().getModuleRegistry().startModule(sensorCfg.id, TIMEOUT);
        
        // create SOS data provider config
        SensorDataProviderConfig provCfg = new SensorDataProviderConfig();
        provCfg.enabled = true;
        provCfg.name = NAME_OFFERING1;
        provCfg.uri = URI_OFFERING1;
        provCfg.sensorID = sensor.getLocalID();
        //provCfg.hiddenOutputs
        
        return provCfg;
    }
    
    
    protected SensorDataProviderConfig buildSensorProvider2() throws Exception
    {
        return buildSensorProvider2(true);
    }
    
    
    protected SensorDataProviderConfig buildSensorProvider2(boolean start) throws Exception
    {
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.autoStart = false;
        sensorCfg.moduleClass = FakeSensorNetWithFoi.class.getCanonicalName();
        sensorCfg.name = "Sensor2";
        FakeSensorNetWithFoi sensor = (FakeSensorNetWithFoi)SensorHub.getInstance().getModuleRegistry().loadModule(sensorCfg);
        sensor.setSensorUID(UID_SENSOR2);
        sensor.setDataInterfaces(
                new FakeSensorData(sensor, NAME_OUTPUT1, 1, SAMPLING_PERIOD, NUM_GEN_SAMPLES),
                new FakeSensorData2(sensor, NAME_OUTPUT2, SAMPLING_PERIOD, NUM_GEN_SAMPLES, obsFoiMap));
        if (start)
            SensorHub.getInstance().getModuleRegistry().startModule(sensorCfg.id, TIMEOUT);
        
        // create SOS data provider config
        SensorDataProviderConfig provCfg = new SensorDataProviderConfig();
        provCfg.enabled = true;
        provCfg.name = NAME_OFFERING2;
        provCfg.uri = URI_OFFERING2;
        provCfg.sensorID = sensor.getLocalID();
        //provCfg.hiddenOutputs;
        
        return provCfg;
    }
    
    
    protected SensorDataProviderConfig buildSensorProvider1WithStorage() throws Exception
    {
        return buildSensorProvider1WithStorage(true);
    }
    
    
    protected SensorDataProviderConfig buildSensorProvider1WithStorage(boolean start) throws Exception
    {
        SensorDataProviderConfig sosProviderConfig = buildSensorProvider1(start);
                       
        // configure in-memory storage
        StreamStorageConfig streamStorageConfig = new StreamStorageConfig();
        streamStorageConfig.name = "Memory Storage";
        streamStorageConfig.autoStart = true;
        streamStorageConfig.storageConfig = new StorageConfig();
        streamStorageConfig.storageConfig.moduleClass = InMemoryBasicStorage.class.getCanonicalName();
        streamStorageConfig.dataSourceID = sosProviderConfig.sensorID;
        
        // start storage module
        IRecordStorageModule<?> storage = (IRecordStorageModule<?>)SensorHub.getInstance().getModuleRegistry().loadModuleAsync(streamStorageConfig, null);
        if (start)
            storage.waitForState(ModuleState.STARTED, TIMEOUT);
        else
            storage.waitForState(ModuleState.STARTING, TIMEOUT);
                
        // configure storage for sensor
        sosProviderConfig.storageID = storage.getLocalID();
        
        return sosProviderConfig;
    }
    
    
    protected SensorDataProviderConfig buildSensorProvider2WithObsStorage() throws Exception
    {
        return buildSensorProvider2WithObsStorage(true);
    }
    
    
    protected SensorDataProviderConfig buildSensorProvider2WithObsStorage(boolean start) throws Exception
    {
        SensorDataProviderConfig sosProviderConfig = buildSensorProvider2(start);
                       
        // configure object DB storage
        StreamStorageConfig streamStorageConfig = new StreamStorageConfig();
        streamStorageConfig.name = "Obs Storage";
        streamStorageConfig.autoStart = true;
        streamStorageConfig.storageConfig = new BasicStorageConfig();
        streamStorageConfig.storageConfig.moduleClass = ObsStorageImpl.class.getCanonicalName();
        streamStorageConfig.storageConfig.storagePath = dbFile.getAbsolutePath();
        streamStorageConfig.dataSourceID = sosProviderConfig.sensorID;
        
        // start storage module
        IRecordStorageModule<?> storage = (IRecordStorageModule<?>)SensorHub.getInstance().getModuleRegistry().loadModule(streamStorageConfig, TIMEOUT);
                
        // configure storage for sensor
        sosProviderConfig.storageID = storage.getLocalID();
        
        return sosProviderConfig;
    }
    
    
    protected FakeSensor startSending(SensorDataProviderConfig sosProviderConfig, boolean waitForFirstRecord) throws Exception
    {
        final ReentrantLock lock = new ReentrantLock();
        final Condition firstRecord = lock.newCondition();
        
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getModuleRegistry().startModule(sosProviderConfig.sensorID, TIMEOUT);
        sensor.getAllOutputs().get(NAME_OUTPUT1).registerListener(new IEventListener() {
            public void handleEvent(Event<?> event)
            { 
                lock.lock();
                firstRecord.signal();
                lock.unlock();
            }
        });
        
        if (waitForFirstRecord)
        {
            lock.lock();
            assertTrue("No data available before timeout", firstRecord.await(10, TimeUnit.SECONDS));
            lock.unlock();
        }
        
        return sensor;
    }
    
    
    protected DOMHelper sendRequest(OWSRequest request, boolean usePost) throws Exception
    {
        OWSUtils utils = new OWSUtils();
        InputStream is;
        
        if (usePost)
        {
            is = utils.sendPostRequest(request).getInputStream();
            utils.writeXMLQuery(System.out, request);
        }
        else
        {
            is = utils.sendGetRequest(request).getInputStream();
            System.out.println(utils.buildURLQuery(request));
        }
        
        DOMHelper dom = new DOMHelper(is, false);
        dom.serialize(dom.getBaseElement(), System.out, true);
        OWSExceptionReader.checkException(dom, dom.getBaseElement());
        return dom;
    }
    
    
    protected DOMHelper sendSoapRequest(OWSRequest request) throws Exception
    {
        OWSUtils utils = new OWSUtils();
        InputStream is = utils.sendSoapRequest(request).getInputStream();
        DOMHelper dom = new DOMHelper(is, false);
        dom.serialize(dom.getBaseElement(), System.out, true);
        OWSExceptionReader.checkException(dom, dom.getBaseElement());
        return dom;
    }
    
    
    protected void checkServiceException(InputStream is, String locator) throws Exception
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(is, os);
        
        try
        {
            ByteArrayInputStream bis = new ByteArrayInputStream(os.toByteArray());
            OGCExceptionReader.parseException(bis);
            assertFalse("Expected service exception", true); // we should never be here
        }
        catch (OGCException e)
        {
            String exceptionXml = os.toString(StandardCharsets.UTF_8.name());
            assertTrue("Wrong exception:\n" + exceptionXml, exceptionXml.contains("locator=\"" + locator + "\""));
        }
    }
    
    
    @Test
    public void testSetupService() throws Exception
    {
        deployService(buildSensorProvider1());
    }
    
    
    @Test
    public void testNoTransactional() throws Exception
    {
        deployService(buildSensorProvider1());
        
        try
        {
            InsertResultRequest req = new InsertResultRequest();
            req.setPostServer(HTTP_ENDPOINT);
            req.setVersion("2.0");
            req.setTemplateId("template01");
            SWEData sweData = new SWEData();
            sweData.setElementType(new QuantityImpl());
            sweData.setEncoding(new TextEncodingImpl(",", " "));
            sweData.addData(new DataBlockDouble(1));
            req.setResultData(sweData);
            new OWSUtils().sendRequest(req, false);
        }
        catch (OWSException e)
        {
            assertTrue(e.getLocator().equals("request"));
        }
    }

    
    protected DOMHelper checkOfferings(InputStream is, String... sensorUIDs) throws Exception
    {
        DOMHelper dom = new DOMHelper(is, false);
        checkOfferings(dom, dom.getBaseElement(), sensorUIDs);
        return dom;
    }
    
    
    protected void checkOfferings(DOMHelper dom, Element baseElt, String... sensorUIDs) throws Exception
    {
        dom.serialize(baseElt, System.out, true);        
        NodeList offeringElts = dom.getElements(baseElt, OFFERING_NODES);
        assertEquals("Wrong number of offerings", sensorUIDs.length, offeringElts.getLength());
        
        int i = 0;
        for (String sensorUID: sensorUIDs)
        {
            String expectedUri = null;
            String expectedName = null;
            
            if (UID_SENSOR1.equals(sensorUID))
            {
                expectedUri = URI_OFFERING1;
                expectedName = NAME_OFFERING1;
            }
            else if (UID_SENSOR2.equals(sensorUID))
            {
                expectedUri = URI_OFFERING2;
                expectedName = NAME_OFFERING2;
            }
            
            assertEquals("Wrong offering id", expectedUri, dom.getElementValue((Element)offeringElts.item(i), "identifier"));
            assertEquals("Wrong offering name", expectedName, dom.getElementValue((Element)offeringElts.item(i), "name"));
            i++;
        }
    }
    
    
    @Test
    public void testGetCapabilitiesOneOffering1() throws Exception
    {
        deployService(buildSensorProvider1());
        InputStream is = new URL(HTTP_ENDPOINT + GETCAPS_REQUEST).openStream();
        checkOfferings(is, new String[] {UID_SENSOR1});
    }
    
    
    @Test
    public void testGetCapabilitiesTwoOfferings() throws Exception
    {
        deployService(buildSensorProvider1(), buildSensorProvider2());
        InputStream is = new URL(HTTP_ENDPOINT + GETCAPS_REQUEST).openStream();
        checkOfferings(is, new String[] {UID_SENSOR1, UID_SENSOR2});
    }
    
    
    @Test
    public void testGetCapabilitiesSoap12() throws Exception
    {
        deployService(buildSensorProvider1());
        
        GetCapabilitiesRequest getCaps = new GetCapabilitiesRequest();
        getCaps.setPostServer(HTTP_ENDPOINT);
        getCaps.setSoapVersion(OWSUtils.SOAP12_URI);
        DOMHelper dom = sendSoapRequest(getCaps);
        
        assertEquals(OWSUtils.SOAP12_URI, dom.getBaseElement().getNamespaceURI());
                
        Element capsElt = dom.getElement("Body/Capabilities");
        checkOfferings(dom, capsElt, new String[] {UID_SENSOR1});
    }
    
    
    @Test
    public void testGetCapabilitiesSoap11() throws Exception
    {
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
        GetCapabilitiesRequest getCaps = new GetCapabilitiesRequest();
        getCaps.setPostServer(HTTP_ENDPOINT);
        getCaps.setSoapVersion(OWSUtils.SOAP11_URI);
        DOMHelper dom = sendSoapRequest(getCaps);
        
        assertEquals(OWSUtils.SOAP11_URI, dom.getBaseElement().getNamespaceURI());
        
        Element capsElt = dom.getElement("Body/Capabilities");
        checkOfferings(dom, capsElt, new String[] {UID_SENSOR1, UID_SENSOR2});
    }
    
    
    @Test
    public void testGetCapabilitiesMissingSource() throws Exception
    {
        // provider with wrong sensorUID
        SensorDataProviderConfig provider1 = buildSensorProvider1();
        provider1.sensorID = "bad_ID";
        deployService(provider1, buildSensorProvider2());
        
        InputStream is = new URL(HTTP_ENDPOINT + GETCAPS_REQUEST).openStream();
        checkOfferings(is, new String[] {UID_SENSOR2});
    }
    
    
    protected void checkOfferingTimeRange(DOMHelper dom, int offeringIndex, String expectedBeginValue, String expectedEndValue) throws ParseException
    {
        NodeList offeringElts = dom.getElements(OFFERING_NODES);
        Element offeringElt = (Element)offeringElts.item(offeringIndex);
        
        boolean isBeginIso = Character.isDigit(expectedBeginValue.charAt(0));
        if (isBeginIso)
        {
            String isoText = dom.getElementValue(offeringElt, "phenomenonTime/TimePeriod/beginPosition");
            double time = new DateTimeFormat().parseIso(isoText);
            double expectedTime = new DateTimeFormat().parseIso(expectedBeginValue);
            assertEquals("Wrong begin time " + isoText, expectedTime, time, 10.0);
        }
        else
            assertEquals("Wrong begin time", expectedBeginValue, dom.getAttributeValue(offeringElt, "phenomenonTime/TimePeriod/beginPosition/indeterminatePosition"));
            
        boolean isEndIso = Character.isDigit(expectedEndValue.charAt(0));
        if (isEndIso)
        {
            String isoText = dom.getElementValue(offeringElt, "phenomenonTime/TimePeriod/endPosition");
            double time = new DateTimeFormat().parseIso(isoText);
            double expectedTime = new DateTimeFormat().parseIso(expectedEndValue);
            assertEquals("Wrong end time " + isoText, expectedTime, time, 10.0);
        }
        else
            assertEquals("Wrong end time", expectedEndValue, dom.getAttributeValue(offeringElt, "phenomenonTime/TimePeriod/endPosition/indeterminatePosition"));
    }
    
    
    @Test
    public void testGetCapabilitiesLiveTimeRange() throws Exception
    {
        SensorDataProviderConfig provider1 = buildSensorProvider1(false);
        SensorDataProviderConfig provider2 = buildSensorProvider2(true);
        provider1.liveDataTimeout = 0.5;
        provider2.liveDataTimeout = 1.0;
        deployService(provider2, provider1);
        
        // wait for timeout
        Thread.sleep(((long)(provider2.liveDataTimeout*1000)));
        
        // sensor1 is not started, sensor2 is started but not sending data
        InputStream is = new URL(HTTP_ENDPOINT + GETCAPS_REQUEST).openStream();
        DOMHelper dom = checkOfferings(is, new String[] {UID_SENSOR2});
        checkOfferingTimeRange(dom, 0, "unknown", "unknown");
        
        // start sensor1
        SensorHub.getInstance().getModuleRegistry().startModule(provider1.sensorID);
        is = new URL(HTTP_ENDPOINT + GETCAPS_REQUEST).openStream();
        dom = checkOfferings(is, new String[] {UID_SENSOR2, UID_SENSOR1});
        checkOfferingTimeRange(dom, 0, "unknown", "unknown");
        checkOfferingTimeRange(dom, 1, "unknown", "unknown");
        
        // trigger measurements from sensor1, wait for measurements and check capabilities again
        startSending(provider1, true);
        is = new URL(HTTP_ENDPOINT + GETCAPS_REQUEST).openStream();
        dom = checkOfferings(is, new String[] {UID_SENSOR2, UID_SENSOR1});
        checkOfferingTimeRange(dom, 0, "unknown", "unknown");
        checkOfferingTimeRange(dom, 1, "now", "now");
        
        // trigger measurements from sensor2, wait for measurements and check capabilities again
        FakeSensor sensor2 = startSending(provider2, true);
        is = new URL(HTTP_ENDPOINT + GETCAPS_REQUEST).openStream();
        dom = checkOfferings(is, new String[] {UID_SENSOR2, UID_SENSOR1});
        checkOfferingTimeRange(dom, 0, "now", "now");
        checkOfferingTimeRange(dom, 1, "now", "now");
        
        // wait until timeout
        while (sensor2.getAllOutputs().get(NAME_OUTPUT1).isEnabled())
            Thread.sleep((long)(SAMPLING_PERIOD*1000));
        Thread.sleep((long)(provider2.liveDataTimeout*1000));
        is = new URL(HTTP_ENDPOINT + GETCAPS_REQUEST).openStream();
        dom = checkOfferings(is, new String[] {UID_SENSOR2, UID_SENSOR1});
        checkOfferingTimeRange(dom, 0, "unknown", "unknown");
        checkOfferingTimeRange(dom, 1, "unknown", "unknown");
    }
    
    
    @Test
    public void testGetCapabilitiesLiveAndHistorical() throws Exception
    {
        SensorDataProviderConfig provider1 = buildSensorProvider1WithStorage(false);
        SensorDataProviderConfig provider2 = buildSensorProvider2WithObsStorage(true);
        provider1.liveDataTimeout = 100.0;
        provider2.liveDataTimeout = 100.0;
        deployService(provider2, provider1);
        
        // wait for at least one record to be in storage
        Thread.sleep(((long)(SAMPLING_PERIOD*1000)));
        
        // sensor1 is not started, sensor2 is started and sending data
        InputStream is = new URL(HTTP_ENDPOINT + GETCAPS_REQUEST).openStream();
        DOMHelper dom = checkOfferings(is, new String[] {UID_SENSOR2});
        String currentIsoTime = new DateTimeFormat().formatIso(System.currentTimeMillis()/1000., 0);
        checkOfferingTimeRange(dom, 0, currentIsoTime, "now");
        
        // start sensor1 and wait for at least one record to be in storage
        SensorHub.getInstance().getModuleRegistry().startModule(provider1.sensorID);
        Thread.sleep(((long)(3*SAMPLING_PERIOD*1000)));
        
        is = new URL(HTTP_ENDPOINT + GETCAPS_REQUEST).openStream();
        dom = checkOfferings(is, new String[] {UID_SENSOR2, UID_SENSOR1});
        currentIsoTime = new DateTimeFormat().formatIso(System.currentTimeMillis()/1000., 0);
        checkOfferingTimeRange(dom, 0, currentIsoTime, "now");
        checkOfferingTimeRange(dom, 1, currentIsoTime, "now");
    }
    
    
    @Test
    public void testGetCapabilitiesLiveAndHistoricalAfterTimeOut() throws Exception
    {
        SensorDataProviderConfig provider1 = buildSensorProvider1WithStorage(true);
        SensorDataProviderConfig provider2 = buildSensorProvider2WithObsStorage(true);
        provider1.liveDataTimeout = 1.0;
        provider2.liveDataTimeout = 100.0;
        deployService(provider2, provider1);
        
        // wait for time out from sensor2
        FakeSensor sensor1 = getSensorModule(1);
        while (sensor1.getAllOutputs().get(NAME_OUTPUT1).isEnabled())
            Thread.sleep((long)(SAMPLING_PERIOD*1000));
        Thread.sleep((long)((SAMPLING_PERIOD+provider1.liveDataTimeout*2)*1000));
        InputStream is = new URL(HTTP_ENDPOINT + GETCAPS_REQUEST).openStream();
        DOMHelper dom = checkOfferings(is, new String[] {UID_SENSOR2, UID_SENSOR1});
        String currentIsoTime = new DateTimeFormat().formatIso(System.currentTimeMillis()/1000., 0);
        checkOfferingTimeRange(dom, 0, currentIsoTime, "now");
        checkOfferingTimeRange(dom, 1, currentIsoTime, currentIsoTime);
    }
    
    
    @Test
    public void testDescribeSensor() throws Exception
    {
        deployService(buildSensorProvider1(), buildSensorProvider2());
        OWSRequest dsReq;
        DOMHelper dom;
        
        dsReq = generateDescribeSensor(UID_SENSOR1);
        dom = sendRequest(dsReq, false);        
        assertEquals(UID_SENSOR1, dom.getElementValue("description/SensorDescription/data/PhysicalSystem/identifier"));
        
        dsReq = generateDescribeSensor(UID_SENSOR2);
        dom = sendRequest(dsReq, false);        
        assertEquals(UID_SENSOR2, dom.getElementValue("description/SensorDescription/data/PhysicalSystem/identifier"));
    }
    
    
    @Test
    public void testDescribeSensorSoap11() throws Exception
    {
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
        OWSRequest request = generateDescribeSensor(UID_SENSOR1);
        request.setSoapVersion(OWSUtils.SOAP11_URI);
        DOMHelper dom = sendSoapRequest(request);
        
        assertEquals(OWSUtils.SOAP11_URI, dom.getBaseElement().getNamespaceURI());        
        assertEquals(UID_SENSOR1, dom.getElementValue("Body/DescribeSensorResponse/description/SensorDescription/data/PhysicalSystem/identifier"));
    }
    
    
    protected String[] sendGetResult(String offering, String observables, String timeRange) throws Exception
    {
        return sendGetResult(offering, observables, timeRange, false);
    }
    
    
    protected String[] sendGetResult(String offering, String observables, String timeRange, boolean useWebsocket) throws Exception
    {
        String url = (useWebsocket ? WS_ENDPOINT : HTTP_ENDPOINT) + 
                "?service=SOS&version=2.0&request=GetResult" + 
                "&offering=" + offering +
                "&observedProperty=" + observables + 
                "&temporalfilter=time," + timeRange;
        
        String currentTime = new DateTimeFormat().formatIso(System.currentTimeMillis()/1000., 0);
        
        if (useWebsocket)
        {
            WebSocketClient client = new WebSocketClient();
            final ReentrantLock lock = new ReentrantLock();
            final Condition endData = lock.newCondition();
            
            class MyWsHandler extends WebSocketAdapter
            {
                ArrayList<String> records = new ArrayList<String>();
                
                public void onWebSocketBinary(byte payload[], int offset, int len)
                {
                    String rec = new String(payload, offset, len);
                    System.out.print("Received: " + rec);
                    records.add(rec);
                }

                public void onWebSocketClose(int arg0, String arg1)
                {
                    lock.lock();
                    try { endData.signalAll(); }
                    finally { lock.unlock(); }
                }            
            };
            
            System.out.println("Sending WebSocket request @ " + currentTime);
            MyWsHandler wsHandler = new MyWsHandler();
            client.start();
            client.connect(wsHandler, new URI(url));
            
            lock.lock();
            try { assertTrue("No data received before timeout", endData.await(5, TimeUnit.SECONDS)); }
            finally { lock.unlock(); }
            
            return wsHandler.records.toArray(new String[0]);
        }
        else
        {
            System.out.println("Sending HTTP GET request @ " + currentTime);
            InputStream is = new URL(url).openStream();
            
            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer);
            String respString = writer.toString(); 
            
            assertFalse("Unexpected XML response received:\n" + respString, respString.startsWith("<?xml"));        
            assertFalse("Response is empty", respString.trim().length() == 0);
            
            System.out.println(respString);
            return respString.split("\n");
        }
    }
    
    
    protected Future<String[]> sendGetResultAsync(final String offering, final String observables, final String timeRange, final boolean useWebsocket) throws Exception
    {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        
        Future<String[]> result = exec.submit(new Callable<String[]>() {
            public String[] call() throws Exception
            {
                return sendGetResult(offering, observables, timeRange, useWebsocket);
            }
        });
        
        return result;
    }
    
    
    protected void checkGetResultResponse(String[] records, int expectedNumRecords, int expectedNumFields)
    {
        assertEquals("Wrong number of records", expectedNumRecords, records.length);
        
        for (String rec: records)
        {
            String[] fields = rec.split(",");
            assertEquals("Wrong number of record fields", expectedNumFields, fields.length);
        }
    }
    
    
    @Test
    public void testGetResultNow() throws Exception
    {
        SensorDataProviderConfig provider1 = buildSensorProvider1();
        deployService(provider1);
        startSending(provider1, true);
        
        String[] records = sendGetResult(URI_OFFERING1, URI_PROP1_FIELD2, TIMERANGE_NOW);
        checkGetResultResponse(records, 1, 2);
    }
    
    
    @Test
    public void testGetResultNowDisabledSensor() throws Exception
    {
        SensorDataProviderConfig provider1 = buildSensorProvider1();
        provider1.liveDataTimeout = 0;
        deployService(provider1);
        
        InputStream is = new URL(HTTP_ENDPOINT + 
                "?service=SOS&version=2.0&request=GetResult"
                + "&offering=" + URI_OFFERING1
                + "&observedProperty=" + URI_PROP1
                + "&temporalfilter=time," + TIMERANGE_NOW).openStream();
                        
        checkServiceException(is, "phenomenonTime");
    }
    
    
    @Test
    public void testGetResultRealTimeAllObservables() throws Exception
    {
        deployService(buildSensorProvider1());      
        
        String[] records = sendGetResult(URI_OFFERING1, URI_PROP1, TIMERANGE_FUTURE);
        checkGetResultResponse(records, NUM_GEN_SAMPLES, 4);
    }
    
    
    @Test
    public void testGetResultRealTimeOneObservable() throws Exception
    {
        deployService(buildSensorProvider1());
                
        String[] records = sendGetResult(URI_OFFERING1, URI_PROP1_FIELD1, TIMERANGE_FUTURE);
        checkGetResultResponse(records, NUM_GEN_SAMPLES, 2);
    }
    
    
    @Test
    public void testGetResultRealTimeTwoObservables() throws Exception
    {
        deployService(buildSensorProvider1());
        
        String[] records = sendGetResult(URI_OFFERING1, URI_PROP1_FIELD1 + "," + URI_PROP1_FIELD2, TIMERANGE_FUTURE);
        checkGetResultResponse(records, NUM_GEN_SAMPLES, 3);
    }
    
    
    @Test
    public void testGetResultRealTimeTwoOfferings() throws Exception
    {
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
        String[] records = sendGetResult(URI_OFFERING1, URI_PROP1, TIMERANGE_FUTURE);
        checkGetResultResponse(records, NUM_GEN_SAMPLES, 4);
        
        records = sendGetResult(URI_OFFERING2, URI_PROP1, TIMERANGE_FUTURE);
        checkGetResultResponse(records, NUM_GEN_SAMPLES, 4);
    }
    
    
    @Test
    public void testGetResultBeforeDataIsAvailable() throws Exception
    {
        deployService(buildSensorProvider1(false));
        FakeSensor sensor1 = getSensorModule(0);
        sensor1.setStartedState();
        
        Future<String[]> future = sendGetResultAsync(URI_OFFERING1, URI_PROP1_FIELD1, TIMERANGE_FUTURE, false);
        
        // actually start sending data after only 1s
        Thread.sleep(1000);
        sensor1.start();

        try
        {
            String[] records = future.get(5, TimeUnit.SECONDS);
            checkGetResultResponse(records, NUM_GEN_SAMPLES, 2);
        }
        catch (Exception e)
        {
            assertTrue("No data received before timeout", false);
        }        
    }
    
    
    @Test
    public void testGetResultWrongOffering() throws Exception
    {
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
        InputStream is = new URL(HTTP_ENDPOINT + 
                "?service=SOS&version=2.0&request=GetResult"
                + "&offering=urn:mysos:wrong"
                + "&observedProperty=urn:blabla:temperature").openStream();
        
        checkServiceException(is, "offering");
    }
    
    
    @Test
    public void testGetResultWrongObservable() throws Exception
    {
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
        InputStream is = new URL(HTTP_ENDPOINT + 
                "?service=SOS&version=2.0&request=GetResult"
                + "&offering=" + URI_OFFERING1
                + "&observedProperty=urn:blabla:wrong").openStream();
                        
        checkServiceException(is, "observedProperty");
    }
    
    
    @Test
    public void testGetResultWebSocketAllObservables() throws Exception
    {
        deployService(buildSensorProvider1());
        
        String[] records = sendGetResult(URI_OFFERING1, URI_PROP1, TIMERANGE_FUTURE, true);
        checkGetResultResponse(records, NUM_GEN_SAMPLES, 4);
    }
    
    
    @Test
    public void testGetResultWebSocketOneObservable() throws Exception
    {
        deployService(buildSensorProvider1());
                
        String[] records = sendGetResult(URI_OFFERING1, URI_PROP1_FIELD1, TIMERANGE_FUTURE, true);
        checkGetResultResponse(records, NUM_GEN_SAMPLES, 2);
    }
    
    
    @Test
    public void testGetResultWebSocketTwoObservables() throws Exception
    {
        deployService(buildSensorProvider1());
                
        String[] records = sendGetResult(URI_OFFERING1, URI_PROP1_FIELD1 + "," + URI_PROP1_FIELD2, TIMERANGE_FUTURE, true);
        checkGetResultResponse(records, NUM_GEN_SAMPLES, 3);
    }
    
    
    @Test
    public void testGetResultWebSocketBeforeDataIsAvailable() throws Exception
    {
        deployService(buildSensorProvider1(false));
        FakeSensor sensor1 = getSensorModule(0);
        sensor1.setStartedState();
        
        Future<String[]> future = sendGetResultAsync(URI_OFFERING1, URI_PROP1_FIELD1, TIMERANGE_FUTURE, true);
        
        // actually start sending data after only 1s
        Thread.sleep(1000);
        sensor1.start();

        try
        {
            String[] records = future.get(5, TimeUnit.SECONDS);
            checkGetResultResponse(records, NUM_GEN_SAMPLES, 2);
        }
        catch (Exception e)
        {
            assertTrue("No data received before timeout", false);
        }        
    }
    
    
    @Test
    public void testGetObsOneOfferingStartNow() throws Exception
    {
        deployService(buildSensorProvider1());
        DOMHelper dom = sendRequest(generateGetObsStartNow(URI_OFFERING1, URI_PROP1), false);
        
        assertEquals("Wrong number of observations returned", NUM_GEN_SAMPLES, dom.getElements("*/OM_Observation").getLength());
    }
    
    
    private FakeSensor getSensorModule(int index)
    {
        Collection<ISensorModule<?>> sensors = SensorHub.getInstance().getSensorManager().getLoadedModules();
        return (FakeSensor)sensors.toArray(new ISensorModule[0])[index];
    }
    
    
    @Test
    public void testGetObsOneOfferingEndNow() throws Exception
    {
        deployService(buildSensorProvider1WithStorage());
        
        // wait until data has been produced and archived
        FakeSensor sensor = getSensorModule(0);
        while (sensor.getAllOutputs().get(NAME_OUTPUT1).isEnabled())
            Thread.sleep(((long)SAMPLING_PERIOD*500));
        
        DOMHelper dom = sendRequest(generateGetObsEndNow(URI_OFFERING1, URI_PROP1), false);        
        assertEquals("Wrong number of observations returned", NUM_GEN_SAMPLES, dom.getElements("*/OM_Observation").getLength());
    }
    
    
    @Test
    public void testGetObsOneOfferingWithTimeRange() throws Exception
    {
        deployService(buildSensorProvider1WithStorage());
        
        // wait until data has been produced and archived
        FakeSensor sensor = getSensorModule(0);;
        while (sensor.getAllOutputs().get(NAME_OUTPUT1).isEnabled())
            Thread.sleep(((long)SAMPLING_PERIOD*500));
                
        // first get capabilities to know available time range
        SOSServiceCapabilities caps = (SOSServiceCapabilities)new OWSUtils().getCapabilities(HTTP_ENDPOINT, "SOS", "2.0");
        TimeExtent timePeriod = ((SOSOfferingCapabilities)caps.getLayer(URI_OFFERING1)).getPhenomenonTime();
        System.out.println("Available time period is " + timePeriod.getIsoString(0));
        
        // then get obs
        double stopTime = System.currentTimeMillis() / 1000.0;
        DOMHelper dom = sendRequest(generateGetObsTimeRange(URI_OFFERING1, URI_PROP1, timePeriod.getStartTime(), stopTime), false);
        assertEquals("Wrong number of observations returned", NUM_GEN_SAMPLES, dom.getElements("*/OM_Observation").getLength());
    }
    
    
    @Test
    public void testGetObsTwoOfferingsWithPost() throws Exception
    {
        deployService(buildSensorProvider1(), buildSensorProvider2());
        DOMHelper dom = sendRequest(generateGetObsStartNow(URI_OFFERING1, URI_PROP1), true);
        
        assertEquals("Wrong number of observations returned", NUM_GEN_SAMPLES, dom.getElements("*/OM_Observation").getLength());
    }
    
    
    @Test
    public void testGetObsTwoOfferingsByFoi() throws Exception
    {
        obsFoiMap.put(1, 1);
        obsFoiMap.put(3, 2);
        obsFoiMap.put(4, 3);
        
        deployService(buildSensorProvider1(), buildSensorProvider2WithObsStorage());
        
        // wait until data has been produced and archived
        FakeSensor sensor = getSensorModule(1);
        while (sensor.getAllOutputs().get(NAME_OUTPUT2).isEnabled())
            Thread.sleep(((long)SAMPLING_PERIOD*500));
        DOMHelper dom;
        
        dom = sendRequest(generateGetObsByFoi(URI_OFFERING2, URI_PROP2, 1), true);        
        assertEquals("Wrong number of observations returned", 2, dom.getElements("*/OM_Observation").getLength());
        
        dom = sendRequest(generateGetObsByFoi(URI_OFFERING2, URI_PROP2, 2), true);        
        assertEquals("Wrong number of observations returned", 1, dom.getElements("*/OM_Observation").getLength());
        
        dom = sendRequest(generateGetObsByFoi(URI_OFFERING2, URI_PROP2, 3), true);        
        assertEquals("Wrong number of observations returned", 2, dom.getElements("*/OM_Observation").getLength());
        
        dom = sendRequest(generateGetObsByFoi(URI_OFFERING2, URI_PROP2, 1, 2), true);        
        assertEquals("Wrong number of observations returned", 3, dom.getElements("*/OM_Observation").getLength());
        
        dom = sendRequest(generateGetObsByFoi(URI_OFFERING2, URI_PROP2, 1, 2, 3), true);        
        assertEquals("Wrong number of observations returned", NUM_GEN_SAMPLES, dom.getElements("*/OM_Observation").getLength());
    }
    
    
    @Test(expected = OGCException.class)
    public void testGetObsWrongFormat() throws Exception
    {
        deployService(buildSensorProvider1());
        
        InputStream is = new URL(HTTP_ENDPOINT + "?service=SOS&version=2.0&request=GetObservation&offering=urn:mysos:sensor1&observedProperty=urn:blabla:temperature&responseFormat=badformat").openStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(is, os);
        
        // read back and print
        ByteArrayInputStream bis = new ByteArrayInputStream(os.toByteArray());
        IOUtils.copy(bis, System.out);
        bis.reset();
        
        // parse and generate exception
        OGCExceptionReader.parseException(bis);
    }
    
    
    protected DescribeSensorRequest generateDescribeSensor(String procId)
    {
        DescribeSensorRequest ds = new DescribeSensorRequest();
        ds.setGetServer(HTTP_ENDPOINT);
        ds.setVersion("2.0");
        ds.setProcedureID(procId);
        return ds;
    }
    
    
    protected GetObservationRequest generateGetObs(String offeringId, String obsProp)
    {
        GetObservationRequest getObs = new GetObservationRequest();
        getObs.setGetServer(HTTP_ENDPOINT);
        getObs.setVersion("2.0");
        getObs.setOffering(offeringId);
        getObs.getObservables().add(obsProp);
        return getObs;
    }
    
    
    protected GetObservationRequest generateGetObsStartNow(String offeringId, String obsProp)
    {
        GetObservationRequest getObs = generateGetObs(offeringId, obsProp);
        double futureTime = System.currentTimeMillis()/1000.0 + 3600.;
        getObs.setTime(TimeExtent.getPeriodStartingNow(futureTime));
        return getObs;
    }
    
    
    protected GetObservationRequest generateGetObsEndNow(String offeringId, String obsProp)
    {
        GetObservationRequest getObs = generateGetObs(offeringId, obsProp);
        double pastTime = System.currentTimeMillis()/1000.0 - 3600.;
        getObs.setTime(TimeExtent.getPeriodEndingNow(pastTime));        
        return getObs;
    }
    
    
    protected GetObservationRequest generateGetObsTimeRange(String offeringId, String obsProp, double beginTime, double endTime)
    {
        GetObservationRequest getObs = generateGetObs(offeringId, obsProp);
        getObs.setTime(new TimeExtent(beginTime, endTime));
        return getObs;
    }
    
    
    protected GetObservationRequest generateGetObsByFoi(String offeringId, String obsProp, int... foiNums)
    {
        GetObservationRequest getObs = generateGetObs(offeringId, obsProp);
        for (int foiNum: foiNums)
            getObs.getFoiIDs().add(FakeSensorNetWithFoi.FOI_UID_PREFIX + foiNum);
        return getObs;
    }
    
    
    // TODO test getresult replay
    
    
    @Test
    public void testGetFoisByID() throws Exception
    {
        obsFoiMap.put(1, 1);
        obsFoiMap.put(3, 2);
        obsFoiMap.put(4, 3);
        
        deployService(buildSensorProvider1(), buildSensorProvider2WithObsStorage());
        
        // wait until data has been produced and archived
        FakeSensor sensor = getSensorModule(1);
        while (sensor.getAllOutputs().get(NAME_OUTPUT2).isEnabled())
            Thread.sleep(((long)SAMPLING_PERIOD*500));        
        
        testGetFoisByID(1);
        testGetFoisByID(2);
        testGetFoisByID(3);
        testGetFoisByID(1, 2);
        testGetFoisByID(1, 3);
        testGetFoisByID(3, 2);
        testGetFoisByID(1, 2, 3);
        testGetFoisByID(2, 3, 1);
    }
    
    
    @Test
    public void testGetFoisByIDSoap() throws Exception
    {
        deployService(buildSensorProvider2WithObsStorage());
                
        GetFeatureOfInterestRequest req = new GetFeatureOfInterestRequest();
        req.setGetServer(HTTP_ENDPOINT);
        req.setVersion("2.0");
        req.setSoapVersion(OWSUtils.SOAP12_URI);
        req.getFoiIDs().add(FakeSensorNetWithFoi.FOI_UID_PREFIX+1);
        DOMHelper dom = sendSoapRequest(req);
        
        assertEquals(OWSUtils.SOAP12_URI, dom.getBaseElement().getNamespaceURI());
        assertEquals("Wrong number of features returned", 1, dom.getElements("*/*").getLength());
    }
    
    
    protected void testGetFoisByID(int... foiNums) throws Exception
    {
        GetFeatureOfInterestRequest req = new GetFeatureOfInterestRequest();
        req.setGetServer(HTTP_ENDPOINT);
        req.setVersion("2.0");
        for (int foiNum: foiNums)
            req.getFoiIDs().add(FakeSensorNetWithFoi.FOI_UID_PREFIX + foiNum);
        
        DOMHelper dom = sendRequest(req, false); 
        assertEquals("Wrong number of features returned", foiNums.length, dom.getElements("*/*").getLength());
        
        NodeList nodes = dom.getElements("*/*");
        for (int i=0; i<nodes.getLength(); i++)
        {
            String fid = dom.getAttributeValue((Element)nodes.item(i), "id");
            assertEquals("F" + foiNums[i], fid);
        }
    }
    
    
    @Test
    public void testGetFoisByBbox() throws Exception
    {
        obsFoiMap.put(2, 1);
        obsFoiMap.put(4, 2);
        obsFoiMap.put(5, 3);
        
        deployService(buildSensorProvider1(), buildSensorProvider2WithObsStorage());
        
        // wait until data has been produced and archived
        FakeSensor sensor = getSensorModule(1);
        while (sensor.getAllOutputs().get(NAME_OUTPUT2).isEnabled())
            Thread.sleep(((long)SAMPLING_PERIOD*500));
        
        testGetFoisByBbox(new Bbox(0.5, 0.5, 0.0, 1.5, 1.5, 0.0), 1);
        testGetFoisByBbox(new Bbox(1.5, 1.5, 0.0, 2.5, 2.5, 0.0), 2);
        testGetFoisByBbox(new Bbox(0.5, 0.5, 0.0, 2.5, 2.5, 0.0), 1, 2);
        testGetFoisByBbox(new Bbox(0.5, 0.5, 0.0, 3.5, 3.5, 0.0), 1, 2, 3);
    }
    
    
    protected void testGetFoisByBbox(Bbox bbox, int... foiNums) throws Exception
    {
        GetFeatureOfInterestRequest req = new GetFeatureOfInterestRequest();
        req.setGetServer(HTTP_ENDPOINT);
        req.setVersion("2.0");
        req.setBbox(bbox);
        
        DOMHelper dom = sendRequest(req, false); 
        assertEquals("Wrong number of features returned", foiNums.length, dom.getElements("*/*").getLength());
        
        NodeList nodes = dom.getElements("*/*");
        for (int i=0; i<nodes.getLength(); i++)
        {
            String fid = dom.getAttributeValue((Element)nodes.item(i), "id");
            assertEquals("F" + foiNums[i], fid);
        }
    }
    
    
    @Test
    public void testGetFoisByProcedure() throws Exception
    {
        obsFoiMap.put(2, 1);
        obsFoiMap.put(4, 2);
        obsFoiMap.put(5, 3);
        
        deployService(buildSensorProvider1(), buildSensorProvider2WithObsStorage());
        
        // wait until data has been produced and archived
        FakeSensor sensor = getSensorModule(1);
        while (sensor.getAllOutputs().get(NAME_OUTPUT2).isEnabled())
            Thread.sleep(((long)SAMPLING_PERIOD*500));
        
        testGetFoisByProcedure(Arrays.asList(UID_SENSOR2), 1, 2, 3);
        testGetFoisByProcedure(Arrays.asList(UID_SENSOR1), 1, 2, 3);
        testGetFoisByProcedure(Arrays.asList(UID_SENSOR1, UID_SENSOR2), 1, 2, 3);
    }
    
    
    protected void testGetFoisByProcedure(List<String> procIDs, int... foiNums) throws Exception
    {
        GetFeatureOfInterestRequest req = new GetFeatureOfInterestRequest();
        req.setGetServer(HTTP_ENDPOINT);
        req.setVersion("2.0");
        req.getProcedures().addAll(procIDs);
        
        DOMHelper dom = sendRequest(req, false); 
        assertEquals("Wrong number of features returned", foiNums.length, dom.getElements("*/*").getLength());
        
        NodeList nodes = dom.getElements("*/*");
        for (int i=0; i<nodes.getLength(); i++)
        {
            String fid = dom.getAttributeValue((Element)nodes.item(i), "id");
            assertEquals("F" + foiNums[i], fid);
        }
    }
    
    
    @Test
    public void testGetFoisByObservables() throws Exception
    {
        obsFoiMap.put(2, 1);
        obsFoiMap.put(4, 2);
        obsFoiMap.put(5, 3);
        
        deployService(buildSensorProvider1(), buildSensorProvider2WithObsStorage());
        
        // wait until data has been produced and archived
        FakeSensor sensor = getSensorModule(1);
        while (sensor.getAllOutputs().get(NAME_OUTPUT2).isEnabled())
            Thread.sleep(((long)SAMPLING_PERIOD*500));
        
        testGetFoisByObservables(Arrays.asList("urn:blabla:image"), 1, 2, 3);
        testGetFoisByObservables(Arrays.asList("urn:blabla:RedChannel"), 1, 2, 3);
        testGetFoisByObservables(Arrays.asList("urn:blabla:GreenChannel"), 1, 2, 3);
        testGetFoisByObservables(Arrays.asList("urn:blabla:BlueChannel"), 1, 2, 3);
        testGetFoisByObservables(Arrays.asList("urn:blabla:weatherData"), 1, 2, 3);
    }
    
    
    protected void testGetFoisByObservables(List<String> obsIDs, int... foiNums) throws Exception
    {
        GetFeatureOfInterestRequest req = new GetFeatureOfInterestRequest();
        req.setGetServer(HTTP_ENDPOINT);
        req.setVersion("2.0");
        req.getObservables().addAll(obsIDs);
        
        DOMHelper dom = sendRequest(req, false); 
        assertEquals("Wrong number of features returned", foiNums.length, dom.getElements("*/*").getLength());
        
        NodeList nodes = dom.getElements("*/*");
        for (int i=0; i<nodes.getLength(); i++)
        {
            String fid = dom.getAttributeValue((Element)nodes.item(i), "id");
            assertEquals("F" + foiNums[i], fid);
        }
    }
    
   
    @After
    public void cleanup()
    {
        try
        {
            if (registry != null)
                registry.shutdown(false, false);
            HttpServer.getInstance().cleanup();
            SensorHub.clearInstance();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (dbFile != null)
                dbFile.delete();
        }
    }
}
