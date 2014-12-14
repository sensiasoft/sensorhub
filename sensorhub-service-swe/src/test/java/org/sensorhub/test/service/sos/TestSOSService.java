/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sos;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.SensorHubConfig;
import org.sensorhub.impl.persistence.InMemoryBasicStorage;
import org.sensorhub.impl.persistence.StorageHelper;
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
import org.vast.ows.OWSException;
import org.vast.ows.OWSExceptionReader;
import org.vast.ows.OWSRequest;
import org.vast.ows.OWSUtils;
import org.vast.ows.sos.GetObservationRequest;
import org.vast.ows.sos.InsertResultRequest;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.ows.sos.SOSServiceCapabilities;
import org.vast.swe.SWEData;
import org.vast.util.TimeExtent;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class TestSOSService
{
    static String SERVICE_ENDPOINT = "/sos";
    static String NAME_OUTPUT1 = "weatherOut";
    static String NAME_OUTPUT2 = "imageOut";
    static String URI_OFFERING1 = "urn:mysos:sensor1";
    static String URI_OFFERING2 = "urn:mysos:sensor2";
    static String NAME_OFFERING1 = "SOS Sensor Provider #1";
    static String NAME_OFFERING2 = "SOS Sensor Provider #2";
    static double SAMPLING_PERIOD = 0.5;
    
    File configFile;
    int totalSampleCount;
    
    
    @Before
    public void setupFramework() throws Exception
    {
        // init sensorhub
        configFile = new File("junit-test.json");
        //configFile = File.createTempFile("junit-config-", ".json");
        configFile.deleteOnExit();
        SensorHub.createInstance(new SensorHubConfig(configFile.getAbsolutePath(), configFile.getParent()));
        
        // start HTTP server
        HttpServerConfig httpConfig = new HttpServerConfig();
        SensorHub.getInstance().getModuleRegistry().loadModule(httpConfig);
        
        // to preload FES stuff (prevents delay in first SOS request)
        new org.geotools.filter.v2_0.FESConfiguration();
    }
    
    
    protected SOSService deployService(SOSProviderConfig... providerConfigs) throws Exception
    {   
        // create service config
        SOSServiceConfig serviceCfg = new SOSServiceConfig();
        serviceCfg.moduleClass = SOSService.class.getCanonicalName();
        serviceCfg.endPoint = SERVICE_ENDPOINT;
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
    
    
    protected SensorDataProviderConfig buildSensorProvider1(boolean pushEnabled) throws Exception
    {
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = true;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        IModule<?> sensor = SensorHub.getInstance().getModuleRegistry().loadModule(sensorCfg);
        
        // add custom interfaces
        totalSampleCount = 5;
        ((FakeSensor)sensor).setDataInterfaces(new FakeSensorData((FakeSensor)sensor, NAME_OUTPUT1, pushEnabled, 10, SAMPLING_PERIOD, totalSampleCount));
        
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
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.name = "Sensor2";
        IModule<?> sensor = SensorHub.getInstance().getModuleRegistry().loadModule(sensorCfg);
        ((FakeSensor)sensor).setDataInterfaces(
                new FakeSensorData((FakeSensor)sensor, NAME_OUTPUT1, false),
                new FakeSensorData2((FakeSensor)sensor, NAME_OUTPUT2, false));
        
        // create SOS data provider config
        SensorDataProviderConfig provCfg = new SensorDataProviderConfig();
        provCfg.enabled = true;
        provCfg.name = NAME_OFFERING2;
        provCfg.uri = URI_OFFERING2;
        provCfg.sensorID = sensor.getLocalID();
        //provCfg.hiddenOutputs;
        
        return provCfg;
    }
    
    
    protected SensorDataProviderConfig buildSensorProvider1WithStorage(boolean pushEnabled) throws Exception
    {
        SensorDataProviderConfig sosProviderConfig = buildSensorProvider1(pushEnabled);
                       
        // create in-memory storage
        StorageConfig storageConfig = new StorageConfig();
        storageConfig.moduleClass = InMemoryBasicStorage.class.getCanonicalName();
        storageConfig.name = "Storage";
        storageConfig.enabled = true;
        
        // configure storage for sensor
        IBasicStorage<?> storage = (IBasicStorage<?>)SensorHub.getInstance().getModuleRegistry().loadModule(storageConfig);
        ISensorModule<?> sensor = SensorHub.getInstance().getSensorManager().getModuleById(sosProviderConfig.sensorID);
        StorageHelper.configureStorageForSensor(sensor, storage, true);
        sosProviderConfig.storageID = storage.getLocalID();
        
        return sosProviderConfig;
    }
    
    
    protected GetObservationRequest generateGetObsNow(String offeringId)
    {
        GetObservationRequest getObs = new GetObservationRequest();
        getObs.setGetServer("http://localhost:8080/sensorhub" + SERVICE_ENDPOINT);
        getObs.setVersion("2.0");
        getObs.setOffering(offeringId);
        getObs.getObservables().add("urn:blabla:temperature");
        TimeExtent reqTime = new TimeExtent();
        reqTime.setBaseAtNow(true);
        getObs.setTime(reqTime);
        return getObs;
    }
    
    
    protected GetObservationRequest generateGetObsTimeRange(String offeringId, double beginTime, double endTime)
    {
        GetObservationRequest getObs = generateGetObsNow(offeringId);
        getObs.setTime(new TimeExtent(beginTime, endTime));
        return getObs;
    }
    
    
    protected DOMHelper sendRequest(OWSRequest request, boolean usePost) throws Exception
    {
        OWSUtils utils = new OWSUtils();
        System.out.println(utils.buildURLQuery(request));
        InputStream is = utils.sendGetRequest(request).getInputStream();
        DOMHelper dom = new DOMHelper(is, false);
        dom.serialize(dom.getBaseElement(), System.out, true);
        OWSExceptionReader.checkException(dom, dom.getBaseElement());
        return dom;
    }
    
    
    @Test
    public void testSetupService() throws Exception
    {
        deployService(buildSensorProvider1(false));
    }
    
    
    @Test
    public void testGetCapabilitiesOneOffering() throws Exception
    {
        deployService(buildSensorProvider1(false));
        
        InputStream is = new URL("http://localhost:8080/sensorhub" + SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetCapabilities").openStream();
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
        deployService(buildSensorProvider1(false), buildSensorProvider2());
        
        InputStream is = new URL("http://localhost:8080/sensorhub" + SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetCapabilities").openStream();
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
    public void testGetResultTwoOfferings() throws Exception
    {
        deployService(buildSensorProvider1(false), buildSensorProvider2());
        
        InputStream is = new URL("http://localhost:8080/sensorhub" + SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetResult&offering=urn:mysos:sensor1&observedProperty=urn:blabla:temperature").openStream();
        IOUtils.copy(is, System.out);
    }
    
    
    @Test(expected = OGCException.class)
    public void testGetResultWrongOffering() throws Exception
    {
        deployService(buildSensorProvider1(false), buildSensorProvider2());
        
        InputStream is = new URL("http://localhost:8080/sensorhub" + SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetResult&offering=urn:mysos:wrong&observedProperty=urn:blabla:temperature").openStream();
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
    public void testGetObsOneOfferingUsingPolling() throws Exception
    {
        deployService(buildSensorProvider1(false));
        DOMHelper dom = sendRequest(generateGetObsNow(URI_OFFERING1), false);
        
        assertEquals("Wrong number of observations returned", totalSampleCount, dom.getElements("*/OM_Observation").getLength());
    }
    
    
    @Test
    public void testGetObsOneOfferingUsingPush() throws Exception
    {
        deployService(buildSensorProvider1(true));
        DOMHelper dom = sendRequest(generateGetObsNow(URI_OFFERING1), false);
        
        assertEquals("Wrong number of observations returned", totalSampleCount, dom.getElements("*/OM_Observation").getLength());
    }
    
    
    @Test
    public void testGetObsOneOfferingWithTimeRange() throws Exception
    {
        deployService(buildSensorProvider1WithStorage(true));
        
        // wait until data has been produced and archived
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getSensorManager().getLoadedModules().get(0);
        while (sensor.getAllOutputs().get(NAME_OUTPUT1).isEnabled())
            Thread.sleep(((long)SAMPLING_PERIOD*500));
        
        // first get capabilities to know
        SOSServiceCapabilities caps = (SOSServiceCapabilities)new OWSUtils().getCapabilities("http://localhost:8080/sensorhub" + SERVICE_ENDPOINT, "SOS", "2.0");
        TimeExtent timePeriod = ((SOSOfferingCapabilities)caps.getLayer(URI_OFFERING1)).getPhenomenonTimes().get(0);
        System.out.println("Available time period is " + timePeriod.getIsoString(0));
        
        DOMHelper dom = sendRequest(generateGetObsTimeRange(URI_OFFERING1, timePeriod.getStartTime(), timePeriod.getStopTime()), false);
        assertEquals("Wrong number of observations returned", totalSampleCount, dom.getElements("*/OM_Observation").getLength());
    }
    
    
    @Test
    public void testGetObsTwoOfferingsUsingPolling() throws Exception
    {
        deployService(buildSensorProvider1(false), buildSensorProvider2());
        DOMHelper dom = sendRequest(generateGetObsNow(URI_OFFERING1), false);
        
        assertEquals("Wrong number of observations returned", totalSampleCount, dom.getElements("*/OM_Observation").getLength());
    }
    
    
    @Test(expected = OGCException.class)
    public void testGetObsWrongFormat() throws Exception
    {
        deployService(buildSensorProvider1(false));
        
        InputStream is = new URL("http://localhost:8080/sensorhub" + SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetObservation&offering=urn:mysos:sensor1&observedProperty=urn:blabla:temperature&responseFormat=badformat").openStream();
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
    public void testNoTransactional() throws Exception
    {
        deployService(buildSensorProvider1(false));
        
        try
        {
            InsertResultRequest req = new InsertResultRequest();
            req.setPostServer("http://localhost:8080/sensorhub" + SERVICE_ENDPOINT);
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

    
    @After
    public void cleanup()
    {
        try
        {
            if (configFile != null)
                configFile.delete();
            SensorHub.getInstance().stop();
            HttpServer.getInstance().cleanup();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
