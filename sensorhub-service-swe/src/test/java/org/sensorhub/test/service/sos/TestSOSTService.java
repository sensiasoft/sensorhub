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
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataStream;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Time;
import net.opengis.swe.v20.Vector;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.SensorHubConfig;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.impl.service.ogc.OGCServiceConfig.CapabilitiesInfo;
import org.sensorhub.impl.service.sos.SOSProviderConfig;
import org.sensorhub.impl.service.sos.SOSService;
import org.sensorhub.impl.service.sos.SOSServiceConfig;
import org.sensorhub.impl.service.sos.SensorDataProviderConfig;
import org.sensorhub.test.sensor.FakeSensor;
import org.sensorhub.test.sensor.FakeSensorData;
import org.vast.data.DataList;
import org.vast.data.DataRecordImpl;
import org.vast.data.QuantityImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.data.TimeImpl;
import org.vast.data.VectorImpl;
import org.vast.ogc.om.IObservation;
import org.vast.ogc.om.ObservationImpl;
import org.vast.ogc.om.ProcedureRef;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSException;
import org.vast.ows.OWSResponse;
import org.vast.ows.OWSUtils;
import org.vast.ows.sos.InsertObservationRequest;
import org.vast.ows.sos.InsertResultTemplateRequest;
import org.vast.ows.sos.InsertSensorRequest;
import org.vast.ows.swe.InsertSensorResponse;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.ows.sos.SOSServiceCapabilities;
import org.vast.ows.sos.SOSUtils;
import org.vast.sensorML.PhysicalSystemImpl;
import org.vast.swe.SWEConstants;
import org.vast.util.TimeExtent;


public class TestSOSTService
{
    static final int SERVER_PORT = 8888;
    private static String SERVICE_ENDPOINT = "/sos";
    private static String SENSOR_UID = "urn:mycompany:mysensor:0001";
    File configFile;
    
    
    protected void setupFramework() throws Exception
    {
        // init sensorhub
        configFile = new File("junit-test.json");
        configFile.deleteOnExit();
        SensorHub.createInstance(new SensorHubConfig(configFile.getAbsolutePath(), configFile.getParent()));
        
        // start HTTP server
        HttpServerConfig httpConfig = new HttpServerConfig();
        httpConfig.httpPort = SERVER_PORT;
        SensorHub.getInstance().getModuleRegistry().loadModule(httpConfig);
    }
    
    
    protected SOSService deployService(SOSProviderConfig... providerConfigs) throws Exception
    {   
        ModuleRegistry registry = SensorHub.getInstance().getModuleRegistry();
                
        // create service config
        SOSServiceConfig serviceCfg = new SOSServiceConfig();
        serviceCfg.moduleClass = SOSService.class.getCanonicalName();
        serviceCfg.endPoint = SERVICE_ENDPOINT;
        serviceCfg.enabled = true;
        serviceCfg.name = "SOS";
        serviceCfg.enableTransactional = true;
        CapabilitiesInfo srvcMetadata = serviceCfg.ogcCapabilitiesInfo;
        srvcMetadata.title = "My SOS Service";
        srvcMetadata.description = "An SOS service automatically deployed by SensorHub";
        srvcMetadata.serviceProvider.setOrganizationName("Test Provider, Inc.");
        srvcMetadata.serviceProvider.setDeliveryPoint("15 MyStreet");
        srvcMetadata.serviceProvider.setCity("MyCity");
        srvcMetadata.serviceProvider.setCountry("MyCountry");
        serviceCfg.dataProviders = new ArrayList<SOSProviderConfig>(Arrays.asList(providerConfigs));
        srvcMetadata.fees = "NONE";
        srvcMetadata.accessConstraints = "NONE";
        
        // load module into registry
        SOSService sos = (SOSService)registry.loadModule(serviceCfg);
        SensorHub.getInstance().getModuleRegistry().saveModulesConfiguration();
        return sos;
    }
    
    
    protected SensorDataProviderConfig buildSensorProvider1() throws Exception
    {
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = true;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        IModule<?> sensor = SensorHub.getInstance().getModuleRegistry().loadModule(sensorCfg);
        String outputName = "out1";
        ((FakeSensor)sensor).setDataInterfaces(new FakeSensorData((FakeSensor)sensor, outputName));
        
        // create SOS data provider config
        SensorDataProviderConfig provCfg = new SensorDataProviderConfig();
        provCfg.enabled = true;
        provCfg.name = "SOS Sensor Provider #1";
        provCfg.uri = "urn:mysos:sensor1";
        provCfg.sensorID = sensor.getLocalID();
        //provCfg.hiddenOutputs;
        
        return provCfg;
    }
    
    
    protected String getSosEndpointUrl()
    {
        return "http://localhost:" + SERVER_PORT + "/sensorhub" + SERVICE_ENDPOINT;
    }
    
    
    protected InsertSensorRequest buildInsertSensor() throws Exception
    {
        // create procedure
        PhysicalSystem procedure = new PhysicalSystemImpl();
        procedure.setName("My weather station");
        procedure.setUniqueIdentifier(SENSOR_UID);
        
        // output 1
        DataStream tempOutput = new DataList();
        procedure .addOutput("tempOut", tempOutput);
        
        DataRecord tempRec = new DataRecordImpl(2);
        tempOutput.setElementType("elt", tempRec);
        
        Time timeTag = new TimeImpl();
        timeTag.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
        timeTag.getUom().setHref(Time.ISO_TIME_UNIT);
        tempRec.addComponent("time", timeTag);
        
        Quantity tempVal = new QuantityImpl();
        tempVal.setDefinition("http://mmisw.org/ont/cf/parameter/air_temperature");
        tempVal.getUom().setCode("Cel");
        tempRec.addComponent("temp", tempVal);
        
        tempOutput.setEncoding(new TextEncodingImpl());
        
        
        // output 2
        DataStream posOutput = new DataList();
        procedure.addOutput("posOut", posOutput);
        
        DataRecord posRec = new DataRecordImpl(2);
        posOutput.setElementType("elt", posRec);
        
        posRec.addComponent("time", timeTag.copy());
        
        Vector posVector = new VectorImpl(3);
        posVector.setDefinition(SWEConstants.DEF_SAMPLING_LOC);
        posVector.setReferenceFrame("http://www.opengis.net/def/crs/EPSG/0/4979");
        posVector.addComponent("lat", new QuantityImpl());
        posVector.addComponent("lon", new QuantityImpl());
        posVector.addComponent("alt", new QuantityImpl());
        posRec.addComponent("pos", posVector);
        
        posOutput.setEncoding(new TextEncodingImpl());
        
        
        // build insert sensor request
        InsertSensorRequest req = new InsertSensorRequest();
        req.setPostServer(getSosEndpointUrl());
        req.setVersion("2.0");        
        req.setProcedureDescription(procedure);
        req.setProcedureDescriptionFormat(InsertSensorRequest.DEFAULT_PROCEDURE_FORMAT);
        req.getObservationTypes().add(IObservation.OBS_TYPE_GENERIC);
        req.getObservationTypes().add(IObservation.OBS_TYPE_RECORD);
        req.getObservableProperties().add(SWEConstants.DEF_SAMPLING_LOC);
        req.getObservableProperties().add("http://mmisw.org/ont/cf/parameter/air_temperature");
        req.getFoiTypes().add("urn:blabla:myfoi1");
        req.getFoiTypes().add("urn:blabla:myfoi2");
        
        return req;
    }
    
    
    protected InsertResultTemplateRequest buildInsertResultTemplate(DataStream output, InsertSensorResponse resp) throws Exception
    {
        // build insert sensor request
        InsertResultTemplateRequest req = new InsertResultTemplateRequest();
        req.setPostServer(getSosEndpointUrl());
        req.setVersion("2.0");
        req.setOffering(resp.getAssignedOffering());
        req.setResultStructure(output.getElementType());
        req.setResultEncoding(output.getEncoding());
        req.setObservationTemplate(new ObservationImpl());
        return req;
    }
    
    
    
    @Test
    public void testSetupService() throws Exception
    {
        setupFramework();
        deployService(buildSensorProvider1());
    }
    
    
    @Test
    public void testInsertSensor() throws Exception
    {
        setupFramework();
        deployService(buildSensorProvider1());
        OWSUtils utils = new OWSUtils();
        InsertSensorRequest req = buildInsertSensor();
        
        try
        {
            utils.writeXMLQuery(System.out, req);
            OWSResponse resp = utils.sendRequest(req, false);
            utils.writeXMLResponse(System.out, resp);
        }
        catch (OWSException e)
        {
            utils.writeXMLException(System.out, "SOS", "2.0", e);
            throw e;
        }
        
        // check capabilities has one more offering
        GetCapabilitiesRequest getCap = new GetCapabilitiesRequest();
        getCap.setService(SOSUtils.SOS);
        getCap.setVersion("2.0");
        getCap.setGetServer(getSosEndpointUrl());
        SOSServiceCapabilities caps = (SOSServiceCapabilities)utils.sendRequest(getCap, false);
        utils.writeXMLResponse(System.out, caps);
        assertEquals(2, caps.getLayers().size());
        
        // check offering has correct properties
        SOSOfferingCapabilities newOffering = (SOSOfferingCapabilities)caps.getLayers().get(1);
        assertEquals(req.getProcedureDescription().getUniqueIdentifier(), newOffering.getProcedures().iterator().next());
    }

    
    @Test
    public void testInsertObservation() throws Exception
    {
        setupFramework();
        deployService(buildSensorProvider1());
        OWSUtils utils = new OWSUtils();
        
        // first register sensor
        utils.sendRequest(buildInsertSensor(), false);
        
        // connect to SOS to listen for new obs
        Thread t = new Thread() {
            public void run()
            {
                try
                {
                    InputStream is = new URL(getSosEndpointUrl() + "?service=SOS&version=2.0&request=GetResult&offering=" + SENSOR_UID + "-sos&observedProperty=urn:blabla:temperature").openStream();
                    IOUtils.copy(is, System.out);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
        t.start();
        
        // create new observation
        IObservation obs = new ObservationImpl();
        obs.setPhenomenonTime(new TimeExtent(System.currentTimeMillis() / 1000.0));
        obs.setResultTime(obs.getPhenomenonTime());
        obs.setProcedure(new ProcedureRef(SENSOR_UID));
        
        // build and send insert observation
        InsertObservationRequest insObs = new InsertObservationRequest();
        insObs.setPostServer(getSosEndpointUrl());
        insObs.setVersion("2.0");
        insObs.setOffering(SENSOR_UID + "-sos");
        insObs.getObservations().add(obs);
        
        utils.writeXMLQuery(System.out, insObs);
        OWSResponse resp = utils.sendRequest(insObs, false);
        utils.writeXMLResponse(System.out, resp);
    }
    
    
    @Test
    public void testInsertResultTemplate() throws Exception
    {
        setupFramework();
        deployService(buildSensorProvider1());
        OWSUtils utils = new OWSUtils();
        
        // first register sensor
        InsertSensorRequest req = buildInsertSensor();
        InsertSensorResponse resp = (InsertSensorResponse)utils.sendRequest(req, false);
        
        // send insert template
        DataStream output = (DataStream)req.getProcedureDescription().getOutputList().get(0);
        utils.sendRequest(buildInsertResultTemplate(output, resp), false);
        output = (DataStream)req.getProcedureDescription().getOutputList().get(1);
        utils.sendRequest(buildInsertResultTemplate(output, resp), false);
        
        // get capabilities
        GetCapabilitiesRequest getCap = new GetCapabilitiesRequest();
        getCap.setService(SOSUtils.SOS);
        getCap.setVersion("2.0");
        getCap.setGetServer(getSosEndpointUrl());
        SOSServiceCapabilities caps = (SOSServiceCapabilities)utils.sendRequest(getCap, false);
        utils.writeXMLResponse(System.out, caps);
        assertEquals(2, caps.getLayers().size());
        
        // check offering now has new observed properties
        SOSOfferingCapabilities newOffering = (SOSOfferingCapabilities)caps.getLayers().get(1);
        assertTrue("Observation types missing", newOffering.getObservationTypes().containsAll(req.getObservationTypes()));
        assertTrue("Observed properties missing", newOffering.getObservableProperties().containsAll(req.getObservableProperties()));
        assertTrue("Procedure format missing", newOffering.getProcedureFormats().contains(req.getProcedureDescriptionFormat()));
    }

    
    //@Test
    public void testInsertResult() throws Exception
    {
        // send insert result
        
        // check we can retrieve data back
        
    }
    
    
    //@Test
    public void testInsertResultWithLiveListener() throws Exception
    {
        // connect getResult client
        
        // send insert result
        
        // check client receives real time data
        
    }
    
    
    @After
    public void cleanup()
    {
        try
        {           
            // also make sure we cleanup all modules to remove all generated files
            ModuleRegistry registry = SensorHub.getInstance().getModuleRegistry();
            for (IModule<?> module: registry.getLoadedModules()) 
            {
                module.stop();
                module.cleanup();
            }
            registry.shutdown(false, false);
            
            configFile.delete();
        }
        catch (Exception e)
        {
        }
    }
}
