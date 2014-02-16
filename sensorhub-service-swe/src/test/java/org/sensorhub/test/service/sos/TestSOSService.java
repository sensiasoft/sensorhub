/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.SensorHubConfig;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.HttpServerConfig;
import org.sensorhub.impl.service.ogc.OGCServiceConfig.CapabilitiesInfo;
import org.sensorhub.impl.service.sos.SOSProviderConfig;
import org.sensorhub.impl.service.sos.SOSService;
import org.sensorhub.impl.service.sos.SOSServiceConfig;
import org.sensorhub.impl.service.sos.SensorDataProviderConfig;
import org.vast.ogc.OGCException;
import org.vast.ogc.OGCExceptionReader;


public class TestSOSService
{
    private static String SERVICE_ENDPOINT = "/sos";
    File configFile;
    
    
    protected void setupFramework() throws Exception
    {
        // init sensorhub
        configFile = new File("junit-test.json");
        configFile.deleteOnExit();
        SensorHub.createInstance(new SensorHubConfig(configFile.getAbsolutePath()));
        
        // start HTTP server
        HttpServer server = HttpServer.getInstance();
        HttpServerConfig config = new HttpServerConfig();
        server.init(config);
        server.start();
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
    
    
    protected SensorDataProviderConfig buildSensorProvider1() throws Exception
    {
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = true;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.name = "Sensor1";
        IModule<?> sensor = SensorHub.getInstance().getModuleRegistry().loadModule(sensorCfg);
        String outputName = "out1";
        ((FakeSensor)sensor).setDataInterfaces(new FakeSensorData((FakeSensor)sensor, outputName, false));
        
        // create SOS data provider config
        SensorDataProviderConfig provCfg = new SensorDataProviderConfig();
        provCfg.enabled = true;
        provCfg.name = "SOS Sensor Provider #1";
        provCfg.uri = "urn:mysos:sensor1";
        provCfg.sensorID = sensor.getLocalID();
        provCfg.hiddenOutputs = new String[] {};
        
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
                new FakeSensorData((FakeSensor)sensor, "weatherOut", false),
                new FakeSensorData2((FakeSensor)sensor, "imgOut", false));
        
        // create SOS data provider config
        SensorDataProviderConfig provCfg = new SensorDataProviderConfig();
        provCfg.enabled = true;
        provCfg.name = "SOS Sensor Provider #2";
        provCfg.uri = "urn:mysos:sensor2";
        provCfg.sensorID = sensor.getLocalID();
        provCfg.hiddenOutputs = new String[] {};
        
        return provCfg;
    }
    
    
    @Test
    public void testSetupService() throws Exception
    {
        setupFramework();
        deployService(buildSensorProvider1());
    }
    
    
    @Test
    public void testGetCapabilitiesOneOffering() throws Exception
    {
        setupFramework();
        deployService(buildSensorProvider1());
        
        InputStream is = new URL("http://localhost:8080/sensorhub" + SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetCapabilities").openStream();
        IOUtils.copy(is, System.out);
    }
    
    
    @Test
    public void testGetCapabilitiesTwoOfferings() throws Exception
    {
        setupFramework();
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
        InputStream is = new URL("http://localhost:8080/sensorhub" + SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetCapabilities").openStream();
        IOUtils.copy(is, System.out);
    }
    
    
    @Test
    public void testGetResultTwoOfferings() throws Exception
    {
        setupFramework();
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
        InputStream is = new URL("http://localhost:8080/sensorhub" + SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetResult&offering=urn:mysos:sensor1&observedProperty=urn:blabla:temperature").openStream();
        IOUtils.copy(is, System.out);
    }
    
    
    @Test(expected = OGCException.class)
    public void testGetResultWrongOffering() throws Exception
    {
        setupFramework();
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
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
    public void testGetObsTwoOfferings() throws Exception
    {
        setupFramework();
        deployService(buildSensorProvider1(), buildSensorProvider2());
        
        InputStream is = new URL("http://localhost:8080/sensorhub" + SERVICE_ENDPOINT + "?service=SOS&version=2.0&request=GetObservation&offering=urn:mysos:sensor1&observedProperty=urn:blabla:temperature").openStream();
        IOUtils.copy(is, System.out);
    }
    
    
    @Test(expected = OGCException.class)
    public void testGetObsWrongFormat() throws Exception
    {
        setupFramework();
        deployService(buildSensorProvider1());
        
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

    
    @After
    public void cleanup()
    {
        try
        {
            configFile.delete();
            HttpServer.getInstance().stop();
        }
        catch (Exception e)
        {
        }
    }
}
