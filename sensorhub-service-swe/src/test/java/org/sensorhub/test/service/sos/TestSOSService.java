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
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.persistence.IRecordStorageModule;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.SensorHubConfig;
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
import org.vast.util.TimeExtent;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class TestSOSService
{
    static String NAME_OUTPUT1 = "weatherOut";
    static String NAME_OUTPUT2 = "imageOut";
    static String UID_SENSOR1 = "urn:sensors:mysensor:001";
    static String UID_SENSOR2 = "urn:sensors:mysensor:002";
    static String URI_OFFERING1 = "urn:mysos:sensor1";
    static String URI_OFFERING2 = "urn:mysos:sensor2";
    static String URI_PROP1 = "urn:blabla:temperature";
    static String URI_PROP2 = "urn:blabla:image";
    static String NAME_OFFERING1 = "SOS Sensor Provider #1";
    static String NAME_OFFERING2 = "SOS Sensor Provider #2";
    static final double SAMPLING_PERIOD = 0.1;
    static final int NUM_GEN_SAMPLES = 5;
    static final int NUM_GEN_FEATURES = 3;
    static final int SERVER_PORT = 8888;
    static final String SERVICE_PATH = "/sos";
    static final String SERVICE_ENDPOINT = "http://localhost:" + SERVER_PORT + "/sensorhub" + SERVICE_PATH;
    static final String DB_PATH = "db.dat";
    
    
    Map<Integer, Integer> obsFoiMap = new HashMap<Integer, Integer>();
    File configFile;
    
    
    @Before
    public void setupFramework() throws Exception
    {
        // init sensorhub
        configFile = new File("junit-test.json");
        //configFile = File.createTempFile("junit-config-", ".json");
        configFile.deleteOnExit();
        new File(DB_PATH).deleteOnExit();
        SensorHub.createInstance(new SensorHubConfig(configFile.getAbsolutePath(), configFile.getParent()));
        
        // start HTTP server
        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.httpPort = SERVER_PORT;
        SensorHub.getInstance().getModuleRegistry().loadModule(httpConfig);
    }
    
    
    protected SOSService deployService(SOSProviderConfig... providerConfigs) throws Exception
    {   
        // create service config
        SOSServiceConfig serviceCfg = new SOSServiceConfig();
        serviceCfg.moduleClass = SOSService.class.getCanonicalName();
        serviceCfg.endPoint = SERVICE_PATH;
        serviceCfg.enabled = true;
        serviceCfg.name = "SOS";
        CapabilitiesInfo srvcMetadata = serviceCfg.ogcCapabilitiesInfo;
        srvcMetadata.title = "My SOS Service";
        srvcMetadata.description = "An SOS service automatically deployed by SensorHub";
        srvcMetadata.serviceProvider.setOrganizationName("Test Provider, Inc.");
        srvcMetadata.serviceProvider.setDeliveryPoint("15 MyStreet");
        srvcMetadata.serviceProvider.setCity("MyCity");
        srvcMetadata.serviceProvider.setCountry("MyCountry");
        serviceCfg.dataProviders = Arrays.asList(providerConfigs);
        srvcMetadata.fees = "NONE";
        srvcMetadata.accessConstraints = "NONE";
        
        // load module into registry
        SOSService sos = (SOSService)SensorHub.getInstance().getModuleRegistry().loadModule(serviceCfg);
        SensorHub.getInstance().getModuleRegistry().saveModulesConfiguration();
        return sos;
    }
    
    
    protected SensorDataProviderConfig buildSensorProvider1() throws Exception
    {
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = true;
        sensorCfg.moduleClass = FakeSensorWithFoi.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getModuleRegistry().loadModule(sensorCfg);
        sensor.setSensorUID(UID_SENSOR1);
        sensor.setDataInterfaces(new FakeSensorData(sensor, NAME_OUTPUT1, 10, SAMPLING_PERIOD, NUM_GEN_SAMPLES));
        
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
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = true;
        sensorCfg.moduleClass = FakeSensorWithFoi.class.getCanonicalName();
        sensorCfg.name = "Sensor2";
        FakeSensorWithFoi sensor = (FakeSensorWithFoi)SensorHub.getInstance().getModuleRegistry().loadModule(sensorCfg);
        sensor.setSensorUID(UID_SENSOR2);
        sensor.setDataInterfaces(
                new FakeSensorData(sensor, NAME_OUTPUT1),
                new FakeSensorData2(sensor, NAME_OUTPUT2, SAMPLING_PERIOD, NUM_GEN_SAMPLES, obsFoiMap));
        
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
        SensorDataProviderConfig sosProviderConfig = buildSensorProvider1();
                       
        // configure in-memory storage configure
        StreamStorageConfig streamStorageConfig = new StreamStorageConfig();
        streamStorageConfig.name = "Storage";
        streamStorageConfig.enabled = true;
        streamStorageConfig.storageConfig = new StorageConfig();
        streamStorageConfig.storageConfig.moduleClass = InMemoryBasicStorage.class.getCanonicalName();
        streamStorageConfig.dataSourceID = sosProviderConfig.sensorID;
        
        // configure storage for sensor
        IRecordStorageModule<?> storage = (IRecordStorageModule<?>)SensorHub.getInstance().getModuleRegistry().loadModule(streamStorageConfig);
        sosProviderConfig.storageID = storage.getLocalID();
        
        return sosProviderConfig;
    }
    
    
    protected SensorDataProviderConfig buildSensorProvider2WithObsStorage() throws Exception
    {
        SensorDataProviderConfig sosProviderConfig = buildSensorProvider2();
                       
        // configure in-memory storage configure
        StreamStorageConfig streamStorageConfig = new StreamStorageConfig();
        streamStorageConfig.name = "Storage";
        streamStorageConfig.enabled = true;
        streamStorageConfig.storageConfig = new BasicStorageConfig();
        streamStorageConfig.storageConfig.moduleClass = ObsStorageImpl.class.getCanonicalName();
        streamStorageConfig.storageConfig.storagePath = DB_PATH;
        streamStorageConfig.dataSourceID = sosProviderConfig.sensorID;
        
        // configure storage for sensor
        IRecordStorageModule<?> storage = (IRecordStorageModule<?>)SensorHub.getInstance().getModuleRegistry().loadModule(streamStorageConfig);
        sosProviderConfig.storageID = storage.getLocalID();
        
        return sosProviderConfig;
    }
    
    
    protected DOMHelper sendRequest(OWSRequest request, boolean usePost) throws Exception
    {
        OWSUtils utils = new OWSUtils();
        System.out.println(utils.buildURLQuery(request));
        InputStream is;
        if (usePost)
            is = utils.sendPostRequest(request).getInputStream();
        else
            is = utils.sendGetRequest(request).getInputStream();
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
            req.setPostServer(SERVICE_ENDPOINT);
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
    
    
    @Test
    public void testGetCapabilitiesOneOffering() throws Exception
    {
        deployService(buildSensorProvider1());
        
        InputStream is = new URL(SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetCapabilities").openStream();
        DOMHelper dom = new DOMHelper(is, false);
        dom.serialize(dom.getBaseElement(), System.out, true);
        
        NodeList offeringElts = dom.getElements("contents/Contents/offering/*");
        assertEquals("Wrong number of offerings", 1, offeringElts.getLength());
        assertEquals("Wrong offering id", URI_OFFERING1, dom.getElementValue((Element)offeringElts.item(0), "identifier"));
        assertEquals("Wrong offering name", NAME_OFFERING1, dom.getElementValue((Element)offeringElts.item(0), "name"));
    }
    
    
    @Test
    public void testGetCapabilitiesTwoOfferings() throws Exception
    {
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
        InputStream is = new URL(SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetCapabilities").openStream();
        DOMHelper dom = new DOMHelper(is, false);
        dom.serialize(dom.getBaseElement(), System.out, true);
        
        NodeList offeringElts = dom.getElements("contents/Contents/offering/*");
        assertEquals("Wrong number of offerings", 2, offeringElts.getLength());
        
        assertEquals("Wrong offering id", URI_OFFERING1, dom.getElementValue((Element)offeringElts.item(0), "identifier"));
        assertEquals("Wrong offering name", NAME_OFFERING1, dom.getElementValue((Element)offeringElts.item(0), "name"));
        
        assertEquals("Wrong offering id", URI_OFFERING2, dom.getElementValue((Element)offeringElts.item(1), "identifier"));
        assertEquals("Wrong offering name", NAME_OFFERING2, dom.getElementValue((Element)offeringElts.item(1), "name"));
    }
    
    
    @Test
    public void testGetCapabilitiesSoap12() throws Exception
    {
        deployService(buildSensorProvider1());
        
        GetCapabilitiesRequest getCaps = new GetCapabilitiesRequest();
        getCaps.setPostServer(SERVICE_ENDPOINT);
        getCaps.setSoapVersion(OWSUtils.SOAP12_URI);
        DOMHelper dom = sendSoapRequest(getCaps);
        
        assertEquals(OWSUtils.SOAP12_URI, dom.getBaseElement().getNamespaceURI());
                
        NodeList offeringElts = dom.getElements("Body/Capabilities/contents/Contents/offering/*");
        assertEquals("Wrong number of offerings", 1, offeringElts.getLength());
        assertEquals("Wrong offering id", URI_OFFERING1, dom.getElementValue((Element)offeringElts.item(0), "identifier"));
        assertEquals("Wrong offering name", NAME_OFFERING1, dom.getElementValue((Element)offeringElts.item(0), "name"));
    }
    
    
    @Test
    public void testGetCapabilitiesSoap11() throws Exception
    {
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
        GetCapabilitiesRequest getCaps = new GetCapabilitiesRequest();
        getCaps.setPostServer(SERVICE_ENDPOINT);
        getCaps.setSoapVersion(OWSUtils.SOAP11_URI);
        DOMHelper dom = sendSoapRequest(getCaps);
        
        assertEquals(OWSUtils.SOAP11_URI, dom.getBaseElement().getNamespaceURI());
        
        NodeList offeringElts = dom.getElements("Body/Capabilities/contents/Contents/offering/*");
        assertEquals("Wrong number of offerings", 2, offeringElts.getLength());
        
        assertEquals("Wrong offering id", URI_OFFERING1, dom.getElementValue((Element)offeringElts.item(0), "identifier"));
        assertEquals("Wrong offering name", NAME_OFFERING1, dom.getElementValue((Element)offeringElts.item(0), "name"));
        
        assertEquals("Wrong offering id", URI_OFFERING2, dom.getElementValue((Element)offeringElts.item(1), "identifier"));
        assertEquals("Wrong offering name", NAME_OFFERING2, dom.getElementValue((Element)offeringElts.item(1), "name"));
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
    
    
    @Test
    public void testGetResultTwoOfferings() throws Exception
    {
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
        InputStream is = new URL(SERVICE_ENDPOINT + 
                "?service=SOS&version=2.0&request=GetResult" + 
                "&offering=" + URI_OFFERING1 +
                "&observedProperty=" + URI_PROP1 + 
                "&temporalfilter=time,now/2055-09-05").openStream();
        
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer);
        System.out.println(writer.toString());
        
        assertEquals("Wrong number of records returned", NUM_GEN_SAMPLES, writer.toString().split("\n").length);
    }
    
    
    @Test(expected = OGCException.class)
    public void testGetResultWrongOffering() throws Exception
    {
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
        InputStream is = new URL(SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetResult&offering=urn:mysos:wrong&observedProperty=urn:blabla:temperature").openStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtils.copy(is, os);
        
        // read back and print
        ByteArrayInputStream bis = new ByteArrayInputStream(os.toByteArray());
        IOUtils.copy(bis, System.out);
        bis.reset();
        
        // parse and generate exception
        OGCExceptionReader.parseException(bis);
    }
    
    
    @Test
    public void testGetObsOneOfferingStartNow() throws Exception
    {
        deployService(buildSensorProvider1());
        DOMHelper dom = sendRequest(generateGetObsStartNow(URI_OFFERING1, URI_PROP1), false);
        
        assertEquals("Wrong number of observations returned", NUM_GEN_SAMPLES, dom.getElements("*/OM_Observation").getLength());
    }
    
    
    @Test
    public void testGetObsOneOfferingEndNow() throws Exception
    {
        deployService(buildSensorProvider1WithStorage());
        
        // wait until data has been produced and archived
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getSensorManager().getLoadedModules().get(0);
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
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getSensorManager().getLoadedModules().get(0);
        while (sensor.getAllOutputs().get(NAME_OUTPUT1).isEnabled())
            Thread.sleep(((long)SAMPLING_PERIOD*500));
                
        // first get capabilities to know available time range
        SOSServiceCapabilities caps = (SOSServiceCapabilities)new OWSUtils().getCapabilities(SERVICE_ENDPOINT, "SOS", "2.0");
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
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getSensorManager().getLoadedModules().get(1);
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
        
        InputStream is = new URL(SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetObservation&offering=urn:mysos:sensor1&observedProperty=urn:blabla:temperature&responseFormat=badformat").openStream();
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
        ds.setGetServer(SERVICE_ENDPOINT);
        ds.setVersion("2.0");
        ds.setProcedureID(procId);
        return ds;
    }
    
    
    protected GetObservationRequest generateGetObs(String offeringId, String obsProp)
    {
        GetObservationRequest getObs = new GetObservationRequest();
        getObs.setGetServer(SERVICE_ENDPOINT);
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
            getObs.getFoiIDs().add(FakeSensorWithFoi.FOI_UID_PREFIX + foiNum);
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
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getSensorManager().getLoadedModules().get(1);
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
        req.setGetServer(SERVICE_ENDPOINT);
        req.setVersion("2.0");
        req.setSoapVersion(OWSUtils.SOAP12_URI);
        req.getFoiIDs().add(FakeSensorWithFoi.FOI_UID_PREFIX+1);
        DOMHelper dom = sendSoapRequest(req);
        
        assertEquals(OWSUtils.SOAP12_URI, dom.getBaseElement().getNamespaceURI());
        assertEquals("Wrong number of features returned", 1, dom.getElements("*/*").getLength());
    }
    
    
    protected void testGetFoisByID(int... foiNums) throws Exception
    {
        GetFeatureOfInterestRequest req = new GetFeatureOfInterestRequest();
        req.setGetServer(SERVICE_ENDPOINT);
        req.setVersion("2.0");
        for (int foiNum: foiNums)
            req.getFoiIDs().add(FakeSensorWithFoi.FOI_UID_PREFIX + foiNum);
        
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
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getSensorManager().getLoadedModules().get(1);
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
        req.setGetServer(SERVICE_ENDPOINT);
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
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getSensorManager().getLoadedModules().get(1);
        while (sensor.getAllOutputs().get(NAME_OUTPUT2).isEnabled())
            Thread.sleep(((long)SAMPLING_PERIOD*500));
        
        testGetFoisByProcedure(Arrays.asList(UID_SENSOR2), 1, 2, 3);
        testGetFoisByProcedure(Arrays.asList(UID_SENSOR1), 1, 2, 3);
        testGetFoisByProcedure(Arrays.asList(UID_SENSOR1, UID_SENSOR2), 1, 2, 3);
    }
    
    
    protected void testGetFoisByProcedure(List<String> procIDs, int... foiNums) throws Exception
    {
        GetFeatureOfInterestRequest req = new GetFeatureOfInterestRequest();
        req.setGetServer(SERVICE_ENDPOINT);
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
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getSensorManager().getLoadedModules().get(1);
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
        req.setGetServer(SERVICE_ENDPOINT);
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
            SensorHub.getInstance().getModuleRegistry().shutdown(false, false);
            HttpServer.getInstance().cleanup();
            if (configFile != null)
                configFile.delete();
            File dbFile = new File(DB_PATH);
            if (dbFile.exists())
                dbFile.delete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
