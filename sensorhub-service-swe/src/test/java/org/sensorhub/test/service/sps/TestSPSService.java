/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sps;

import static org.junit.Assert.*;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.SensorHubConfig;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.impl.service.ogc.OGCServiceConfig.CapabilitiesInfo;
import org.sensorhub.impl.service.sps.SPSConnectorConfig;
import org.sensorhub.impl.service.sps.SPSService;
import org.sensorhub.impl.service.sps.SPSServiceConfig;
import org.sensorhub.impl.service.sps.SensorConnectorConfig;
import org.sensorhub.test.sensor.FakeSensor;
import org.sensorhub.test.sensor.FakeSensorControl1;
import org.sensorhub.test.sensor.FakeSensorControl2;
import org.sensorhub.test.sensor.FakeSensorData;
import org.vast.data.DataBlockDouble;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataBlockString;
import org.vast.data.TextEncodingImpl;
import org.vast.ows.OWSException;
import org.vast.ows.OWSExceptionReader;
import org.vast.ows.OWSRequest;
import org.vast.ows.OWSUtils;
import org.vast.ows.sps.DescribeTaskingRequest;
import org.vast.ows.sps.DescribeTaskingResponse;
import org.vast.ows.sps.SPSUtils;
import org.vast.ows.sps.SubmitRequest;
import org.vast.ows.swe.DescribeSensorRequest;
import org.vast.swe.SWEData;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class TestSPSService
{
    static final int SERVER_PORT = 8888;
    static String SERVICE_ENDPOINT = "/sps";
    static String SERVER_URL = "http://localhost:" + SERVER_PORT + "/sensorhub" + SERVICE_ENDPOINT;    
    static String NAME_INPUT1 = "command";
    static String URI_OFFERING1 = "urn:mysps:sensor1";
    static String URI_OFFERING2 = "urn:mysps:sensor2";
    static String NAME_OFFERING1 = "SPS Sensor Control #1";
    static String NAME_OFFERING2 = "SPS Sensor Control #2";
    static String SENSOR_UID_1 = "urn:mysensors:SENSOR001";
    static String SENSOR_UID_2 = "urn:mysensors:SENSOR002";
    
    File configFile;
    
    
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
        httpConfig.httpPort = SERVER_PORT;
        SensorHub.getInstance().getModuleRegistry().loadModule(httpConfig);
    }
    
    
    protected SPSService deployService(SPSConnectorConfig... connectorConfigs) throws Exception
    {   
        // create service config
        SPSServiceConfig serviceCfg = new SPSServiceConfig();
        serviceCfg.moduleClass = SPSService.class.getCanonicalName();
        serviceCfg.endPoint = SERVICE_ENDPOINT;
        serviceCfg.enabled = true;
        serviceCfg.name = "SPS";
        
        CapabilitiesInfo srvcMetadata = serviceCfg.ogcCapabilitiesInfo;
        srvcMetadata.title = "My SPS Service";
        srvcMetadata.description = "An SPS service automatically deployed by SensorHub";
        srvcMetadata.serviceProvider.setOrganizationName("Test Provider, Inc.");
        srvcMetadata.serviceProvider.setDeliveryPoint("15 MyStreet");
        srvcMetadata.serviceProvider.setCity("MyCity");
        srvcMetadata.serviceProvider.setCountry("MyCountry");
        srvcMetadata.fees = "NONE";
        srvcMetadata.accessConstraints = "NONE";
        
        serviceCfg.connectors = Arrays.asList(connectorConfigs);
        
        // load module into registry
        SPSService sps = (SPSService)SensorHub.getInstance().getModuleRegistry().loadModule(serviceCfg);
        SensorHub.getInstance().getModuleRegistry().saveModulesConfiguration();
        return sps;
    }
    
    
    protected SensorConnectorConfig buildSensorConnector1() throws Exception
    {
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = true;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getModuleRegistry().loadModule(sensorCfg);
        sensor.setSensorUID(SENSOR_UID_1);
        
        // add custom interfaces
        ((FakeSensor)sensor).setDataInterfaces(new FakeSensorData(sensor, "output1", 10, 1.0, 0));
        ((FakeSensor)sensor).setControlInterfaces(new FakeSensorControl1(sensor));
        
        // create SOS data provider config
        SensorConnectorConfig provCfg = new SensorConnectorConfig();
        provCfg.enabled = true;
        provCfg.name = NAME_OFFERING1;
        provCfg.uri = URI_OFFERING1;
        provCfg.sensorID = sensor.getLocalID();
        //provCfg.hiddenOutputs
        
        return provCfg;
    }
    
    
    protected SensorConnectorConfig buildSensorConnector2() throws Exception
    {
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = true;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.name = "Sensor2";
        FakeSensor sensor = (FakeSensor)SensorHub.getInstance().getModuleRegistry().loadModule(sensorCfg);
        sensor.setSensorUID(SENSOR_UID_2);
        
        // add custom interfaces
        ((FakeSensor)sensor).setDataInterfaces(new FakeSensorData(sensor, "output1", 10, 1.0, 0));
        ((FakeSensor)sensor).setControlInterfaces(new FakeSensorControl1(sensor), new FakeSensorControl2(sensor));
        
        // create SOS data provider config
        SensorConnectorConfig provCfg = new SensorConnectorConfig();
        provCfg.enabled = true;
        provCfg.name = NAME_OFFERING2;
        provCfg.uri = URI_OFFERING2;
        provCfg.sensorID = sensor.getLocalID();
        //provCfg.hiddenOutputs
        
        return provCfg;
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
    
    
    protected DescribeTaskingRequest generateDescribeTasking(String procedureId)
    {
        DescribeTaskingRequest dtReq = new DescribeTaskingRequest();
        dtReq.setGetServer(SERVER_URL);
        dtReq.setPostServer(SERVER_URL);
        dtReq.setVersion("2.0");
        dtReq.setProcedureID(procedureId);
        return dtReq;
    }
    
    
    protected DescribeSensorRequest generateDescribeSensor(String procedureId)
    {
        DescribeSensorRequest dsReq = new DescribeSensorRequest();
        dsReq.setService(SPSUtils.SPS);
        dsReq.setGetServer(SERVER_URL);
        dsReq.setPostServer(SERVER_URL);
        dsReq.setVersion("2.0");
        dsReq.setProcedureID(procedureId);
        return dsReq;
    } 
    
    
    protected SubmitRequest generateSubmit(String procedureId, DataComponent components, DataBlock... dataBlks)
    {
        SubmitRequest subReq = new SubmitRequest();
        subReq.setPostServer(SERVER_URL);
        subReq.setVersion("2.0");
        subReq.setProcedureID(procedureId);
        SWEData paramData = new SWEData();
        paramData.setElementType(components);
        paramData.setEncoding(new TextEncodingImpl());
        for (DataBlock dataBlock: dataBlks)
            paramData.addData(dataBlock);
        subReq.setParameters(paramData);
        return subReq;
    } 
    
    
    @Test
    public void testSetupService() throws Exception
    {
        deployService(buildSensorConnector1());
    }
    
    
    @Test
    public void testGetCapabilitiesOneOffering() throws Exception
    {
        deployService(buildSensorConnector1());
        
        InputStream is = new URL(SERVER_URL + "?service=SPS&version=2.0&request=GetCapabilities").openStream();
        DOMHelper dom = new DOMHelper(is, false);
        dom.serialize(dom.getBaseElement(), System.out, true);
        
        NodeList offeringElts = dom.getElements("contents/SPSContents/offering/*");
        assertEquals("Wrong number of offerings", 1, offeringElts.getLength());
        assertEquals("Wrong offering id", URI_OFFERING1, dom.getElementValue((Element)offeringElts.item(0), "identifier"));
        assertEquals("Wrong offering name", NAME_OFFERING1, dom.getElementValue((Element)offeringElts.item(0), "name"));
    }
    
    
    @Test
    public void testGetCapabilitiesTwoOfferings() throws Exception
    {
        deployService(buildSensorConnector1(), buildSensorConnector2());
        
        InputStream is = new URL(SERVER_URL + "?service=SOS&version=2.0&request=GetCapabilities").openStream();
        DOMHelper dom = new DOMHelper(is, false);
        dom.serialize(dom.getBaseElement(), System.out, true);
        
        NodeList offeringElts = dom.getElements("contents/SPSContents/offering/*");
        assertEquals("Wrong number of offerings", 2, offeringElts.getLength());
        
        assertEquals("Wrong offering id", URI_OFFERING1, dom.getElementValue((Element)offeringElts.item(0), "identifier"));
        assertEquals("Wrong offering name", NAME_OFFERING1, dom.getElementValue((Element)offeringElts.item(0), "name"));
        
        assertEquals("Wrong offering id", URI_OFFERING2, dom.getElementValue((Element)offeringElts.item(1), "identifier"));
        assertEquals("Wrong offering name", NAME_OFFERING2, dom.getElementValue((Element)offeringElts.item(1), "name"));
    }
    
    
    @Test
    public void testDescribeSensorOneOffering() throws Exception
    {
        deployService(buildSensorConnector1());
        DOMHelper dom = sendRequest(generateDescribeSensor(SENSOR_UID_1), false);
        assertEquals("Wrong Sensor UID", SENSOR_UID_1, dom.getElementValue("identifier"));
    }
    
    
    @Test
    public void testDescribeSensorTwoOfferings() throws Exception
    {
        deployService(buildSensorConnector1(), buildSensorConnector2());
        DOMHelper dom;
        
        dom = sendRequest(generateDescribeSensor(SENSOR_UID_1), false);
        assertEquals("Wrong Sensor UID", SENSOR_UID_1, dom.getElementValue("identifier"));
        assertEquals("Wrong number of control parameters", 1, dom.getElements("parameters/*/parameter").getLength());
        
        dom = sendRequest(generateDescribeSensor(SENSOR_UID_2), true);
        assertEquals("Wrong Sensor UID", SENSOR_UID_2, dom.getElementValue("identifier"));
        assertEquals("Wrong number of control parameters", 2, dom.getElements("parameters/*/parameter").getLength());
    }
    
    
    @Test(expected = OWSException.class)
    public void testDescribeSensorWrongFormat() throws Exception
    {
        deployService(buildSensorConnector1());
        DescribeSensorRequest req = generateDescribeSensor(SENSOR_UID_1);
        req.setFormat("InvalidFormat");
        sendRequest(req, false);
    }
    
    
    @Test
    public void testDescribeTaskingOneOffering() throws Exception
    {
        deployService(buildSensorConnector1());
        DOMHelper dom = sendRequest(generateDescribeTasking(SENSOR_UID_1), false);
        
        NodeList offeringElts = dom.getElements("taskingParameters/*/field");
        assertEquals("Wrong number of tasking parameters", 2, offeringElts.getLength());
    }
    
    
    @Test
    public void testDescribeTaskingTwoOfferings() throws Exception
    {
        deployService(buildSensorConnector1(), buildSensorConnector2());
        
        DOMHelper dom;
        NodeList offeringElts;
        
        dom = sendRequest(generateDescribeTasking(SENSOR_UID_1), false);
        offeringElts = dom.getElements("taskingParameters/*/field");
        assertEquals("Wrong number of tasking parameters", 2, offeringElts.getLength());
        
        dom = sendRequest(generateDescribeTasking(SENSOR_UID_2), true);
        offeringElts = dom.getElements("taskingParameters/*/item");
        assertEquals("Wrong number of parameter choices", 2, offeringElts.getLength());
    }
    
    
    @Test
    public void testSubmitOneOffering() throws Exception
    {
        deployService(buildSensorConnector1());
        SPSUtils spsUtils = new SPSUtils();
        
        // first send describeTasking
        DOMHelper dom = sendRequest(generateDescribeTasking(SENSOR_UID_1), true);
        DescribeTaskingResponse resp = spsUtils.readDescribeTaskingResponse(dom, dom.getBaseElement());
        
        // then send submit
        DataBlock dataBlock = new DataBlockMixed(new DataBlockDouble(1), new DataBlockString(1));
        dataBlock.setDoubleValue(0, 10.0);
        dataBlock.setStringValue(1, "HIGH");
        SubmitRequest subReq = generateSubmit(SENSOR_UID_1, resp.getTaskingParameters(), dataBlock);
        dom = sendRequest(subReq, true);
        
        OWSExceptionReader.checkException(dom, dom.getBaseElement());
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
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
