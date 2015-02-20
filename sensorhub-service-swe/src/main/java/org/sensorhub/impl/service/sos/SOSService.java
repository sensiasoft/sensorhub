/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.BinaryMember;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.persistence.SensorStorageHelper;
import org.sensorhub.impl.persistence.StorageHelper;
import org.sensorhub.impl.sensor.sost.SOSVirtualSensorConfig;
import org.sensorhub.impl.sensor.sost.SOSVirtualSensor;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.ogc.OGCServiceConfig.CapabilitiesInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.cdm.common.DataSource;
import org.vast.cdm.common.DataStreamParser;
import org.vast.cdm.common.DataStreamWriter;
import org.vast.data.DataBlockMixed;
import org.vast.ogc.om.IObservation;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSExceptionReport;
import org.vast.ows.OWSLayerCapabilities;
import org.vast.ows.OWSRequest;
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.GetResultRequest;
import org.vast.ows.sos.ISOSDataConsumer;
import org.vast.ows.sos.ISOSDataConsumer.Template;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.ows.sos.ISOSDataProviderFactory;
import org.vast.ows.sos.InsertObservationRequest;
import org.vast.ows.sos.InsertObservationResponse;
import org.vast.ows.sos.InsertResultRequest;
import org.vast.ows.sos.InsertResultResponse;
import org.vast.ows.sos.InsertResultTemplateRequest;
import org.vast.ows.sos.InsertResultTemplateResponse;
import org.vast.ows.sos.InsertSensorRequest;
import org.vast.ows.sos.InsertSensorResponse;
import org.vast.ows.sos.SOSException;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.ows.sos.SOSServiceCapabilities;
import org.vast.ows.sos.SOSServlet;
import org.vast.ows.sos.SOSUtils;
import org.vast.ows.swe.DeleteSensorRequest;
import org.vast.ows.swe.DeleteSensorResponse;
import org.vast.ows.swe.DescribeSensorRequest;
import org.vast.ows.swe.SWESOfferingCapabilities;
import org.vast.ows.swe.UpdateSensorRequest;
import org.vast.ows.swe.UpdateSensorResponse;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.DataSourceDOM;
import org.vast.swe.SWEFactory;
import org.vast.util.ReaderException;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Implementation of SensorHub generic SOS service.
 * This service is automatically configured (mostly) from information obtained
 * from the selected data sources (sensors, storages, processes, etc).
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
@SuppressWarnings("serial")
public class SOSService extends SOSServlet implements IServiceModule<SOSServiceConfig>, IEventListener
{
    private static final Logger log = LoggerFactory.getLogger(SOSService.class);
    protected static final String invalidWSRequestMsg = "Invalid WebSocket request: ";
    
    SOSServiceConfig config;
    SOSServiceCapabilities capabilitiesCache;
    Map<String, SOSOfferingCapabilities> offeringCaps;
    Map<String, String> procedureToOfferingMap;
    Map<String, String> templateToOfferingMap;
    Map<String, ISOSDataConsumer> dataConsumers;
        
    boolean needCapabilitiesTimeUpdate = false;

    
    @Override
    public boolean isEnabled()
    {
        return config.enabled;
    }
    
    
    @Override
    public void init(SOSServiceConfig config) throws SensorHubException
    {        
        this.config = config;
    }
    
    
    @Override
    public void updateConfig(SOSServiceConfig config) throws SensorHubException
    {
        // cleanup all previously instantiated providers        
        
        // rebuild everything

    }    
    
    
    /**
     * Generates the SOSServiceCapabilities object with info from data source
     * @return
     */
    protected SOSServiceCapabilities generateCapabilities()
    {
        dataProviders.clear();
        procedureToOfferingMap.clear();
        templateToOfferingMap.clear();
        offeringCaps.clear();
        
        // get main capabilities info from config
        CapabilitiesInfo serviceInfo = config.ogcCapabilitiesInfo;
        SOSServiceCapabilities capabilities = new SOSServiceCapabilities();
        capabilities.getIdentification().setTitle(serviceInfo.title);
        capabilities.getIdentification().setDescription(serviceInfo.description);
        capabilities.setFees(serviceInfo.fees);
        capabilities.setAccessConstraints(serviceInfo.accessConstraints);
        capabilities.setServiceProvider(serviceInfo.serviceProvider);
        
        // generate profile list
        capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_RESULT_RETRIEVAL);
        if (config.enableTransactional)
        {
            capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_RESULT_INSERTION);
            capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_OBS_INSERTION);
        }
        
        // process each provider config
        if (config.dataProviders != null)
        {
            for (SOSProviderConfig providerConf: config.dataProviders)
            {
                try
                {
                    // instantiate provider factories and map them to offering URIs
                    IDataProviderFactory provider = providerConf.getFactory();
                    if (!provider.isEnabled())
                        continue;
                                    
                    dataProviders.put(providerConf.uri, provider);
         
                    // add offering metadata to capabilities
                    SOSOfferingCapabilities offCaps = provider.generateCapabilities();
                    capabilities.getLayers().add(offCaps);
                    offeringCaps.put(offCaps.getIdentifier(), offCaps);
                    
                    // build procedure-offering map
                    procedureToOfferingMap.put(offCaps.getProcedures().get(0), offCaps.getIdentifier());
                    
                    if (log.isDebugEnabled())
                        log.debug("Offering " + "\"" + offCaps.toString() + "\" generated for procedure " + offCaps.getProcedures().get(0));
                }
                catch (Exception e)
                {
                    log.error("Error while initializing provider " + providerConf.uri, e);
                }
            }
        }
        
        // process each consumer config
        if (config.dataConsumers != null)
        {
            for (SOSConsumerConfig consumerConf: config.dataConsumers)
            {
                try
                {
                    // for now we support only virtual sensors as consumers
                    ISOSDataConsumer consumer = consumerConf.getConsumerInstance();
                    dataConsumers.put(consumerConf.offering, consumer);
                }
                catch (SensorHubException e)
                {
                    log.error("Error while initializing consumer " + consumerConf.offering, e);
                }
            }
        }
        
        capabilitiesCache = capabilities;
        return capabilities;
    }
    
    
    /**
     * Retrieves SensorML object for the given procedure unique ID
     * @param uri
     * @return
     */
    protected AbstractProcess generateSensorML(String uri, TimeExtent timeExtent) throws ServiceException
    {
        try
        {
            IDataProviderFactory factory = getDataProviderFactoryBySensorID(uri);
            double time = Double.NaN;
            if (timeExtent != null)
                time = timeExtent.getBaseTime();
            return factory.generateSensorMLDescription(time);
        }
        catch (Exception e)
        {
            throw new ServiceException("Error while retrieving SensorML description for sensor " + uri, e);
        }
    }
    
    
    @Override
    public void start()
    {
        this.dataConsumers = new LinkedHashMap<String, ISOSDataConsumer>();
        this.procedureToOfferingMap = new HashMap<String, String>();
        this.templateToOfferingMap = new HashMap<String, String>();
        this.offeringCaps = new HashMap<String, SOSOfferingCapabilities>();
        
        // pre-generate capabilities
        this.capabilitiesCache = generateCapabilities();
                
        // subscribe to server lifecycle events
        SensorHub.getInstance().registerListener(this);
        
        // deploy servlet
        deploy();
    }
    
    
    @Override
    public void stop()
    {
        // undeploy servlet
        undeploy();
        
        // unregister ourself
        SensorHub.getInstance().unregisterListener(this);
        
        // clean all providers
        for (ISOSDataProviderFactory provider: dataProviders.values())
            ((IDataProviderFactory)provider).cleanup();
    }
   
    
    protected void deploy()
    {
        if (!HttpServer.getInstance().isEnabled())
            return;
        
        // deploy ourself to HTTP server
        HttpServer.getInstance().deployServlet(config.endPoint, this);
    }
    
    
    protected void undeploy()
    {
        if (!HttpServer.getInstance().isEnabled())
            return;
        
        HttpServer.getInstance().undeployServlet(this);
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {
        // TODO destroy all virtual sensors
        //for (SOSConsumerConfig consumerConf: config.dataConsumers)
        //    SensorHub.getInstance().getModuleRegistry().destroyModule(consumerConf.sensorID);
    }
    
    
    @Override
    public void handleEvent(Event e)
    {
        // what's important here is to redeploy if HTTP server is restarted
        if (e instanceof ModuleEvent && e.getSource() == HttpServer.getInstance())
        {
            // start when HTTP server is enabled
            if (((ModuleEvent) e).type == ModuleEvent.Type.ENABLED)
                start();
            
            // stop when HTTP server is disabled
            else if (((ModuleEvent) e).type == ModuleEvent.Type.DISABLED)
                stop();
        }
    }
    
    
    @Override
    public SOSServiceConfig getConfiguration()
    {
        return config;
    }
    
    
    @Override
    public String getName()
    {
        return config.name;
    }
    
    
    @Override
    public String getLocalID()
    {
        return config.id;
    }


    @Override
    public void saveState(IModuleStateSaver saver) throws SensorHubException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void loadState(IModuleStateLoader loader) throws SensorHubException
    {
        // TODO Auto-generated method stub
    }
    
    
    /////////////////////////////////////////
    /// methods overriden from SOSServlet ///
    /////////////////////////////////////////
    private WebSocketServletFactory factory = new WebSocketServerFactory();
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // check if we have an upgrade request for websockets
        if (factory.isUpgradeRequest(req, resp))
        {
            // parse request
            OWSRequest owsReq = null;
            try
            {
                owsReq = this.parseRequest(req, resp, false);
                
                if (owsReq != null)
                {
                    // send error if request is not supported via websockets
                    if (!(owsReq instanceof GetResultRequest))
                    {
                        String errorMsg = invalidWSRequestMsg + owsReq.getOperation() + " is not supported via this protocol.";
                        resp.sendError(400, errorMsg);
                        log.trace(errorMsg);
                        owsReq = null;
                    }
                }
            }
            catch (Exception e)
            {
            }
            
            // if SOS request was accepted, create websocket instance
            // and start streaming / accepting incoming stream
            if (owsReq != null)
            {
                SOSWebSocket socketCreator = new SOSWebSocket(this, owsReq);                
                if (factory.acceptWebSocket(socketCreator, req, resp))
                {
                    // We have a socket instance created
                    return;
                }
            }

            return;
        }
        
        // otherwise process as classical HTTP request
        super.service(req, resp);
    }


    @Override
    public void handleRequest(OWSRequest request) throws Exception
    {
        super.handleRequest(request);
    }


    @Override
    protected void handleRequest(GetCapabilitiesRequest request) throws Exception
    {
        // ask providers to refresh their capabilities if needed.
        // we do that here so capabilities doc contains the most up-to-date info.
        // we don't always do it when changes occur because high frequency changes 
        // would trigger too many updates (e.g. new measurements changing time periods)
        for (ISOSDataProviderFactory provider: dataProviders.values())
            ((IDataProviderFactory)provider).updateCapabilities();
        
        sendResponse(request, capabilitiesCache);
    }
        
    
    @Override
    protected void handleRequest(DescribeSensorRequest request) throws Exception
    {
        String sensorID = request.getProcedureID();
                
        // check query parameters        
        OWSExceptionReport report = new OWSExceptionReport();        
        checkQueryProcedure(sensorID, report);
        checkQueryProcedureFormat(procedureToOfferingMap.get(sensorID), request.getFormat(), report);
        report.process();
        
        // serialize and send SensorML description
        OutputStream os = new BufferedOutputStream(request.getResponseStream());
        new SMLUtils().writeProcess(os, generateSensorML(sensorID, request.getTime()), true);
    }
    
    
    @Override
    protected void handleRequest(InsertSensorRequest request) throws Exception
    {
        try
        {
            checkTransactionalSupport(request);
            
            // check query parameters
            OWSExceptionReport report = new OWSExceptionReport();
            checkSensorML(request.getProcedureDescription(), report);
            report.process();
            
            // choose offering name (here derived from sensor ID)
            String sensorUID = request.getProcedureDescription().getUniqueIdentifier();
            if (sensorUID == null)
                throw new SOSException(SOSException.missing_param_code, "identifier", null, "Missing unique identifier in SensorML description");
                        
            // add new offering, provider and virtual sensor if sensor is not already registered
            String offering = procedureToOfferingMap.get(sensorUID);
            if (offering == null)
            {
                offering = sensorUID + "-sos";
                ModuleRegistry moduleReg = SensorHub.getInstance().getModuleRegistry();           
                
                // create and register new virtual sensor module as data consumer
                SOSVirtualSensorConfig sensorConfig = new SOSVirtualSensorConfig();
                sensorConfig.enabled = false;
                sensorConfig.sensorUID = sensorUID;
                sensorConfig.name = request.getProcedureDescription().getName();
                if (sensorConfig.name == null)
                    sensorConfig.name = request.getProcedureDescription().getId();
                SOSVirtualSensor virtualSensor = (SOSVirtualSensor)moduleReg.loadModule(sensorConfig);            
                virtualSensor.updateSensorDescription(request.getProcedureDescription(), true);
                SensorHub.getInstance().getModuleRegistry().enableModule(virtualSensor.getLocalID());
                                
                // generate new provider and consumer config
                SensorDataProviderConfig providerConfig = new SensorDataProviderConfig();
                providerConfig.enabled = true;
                providerConfig.sensorID = virtualSensor.getLocalID();
                providerConfig.uri = offering;
                config.dataProviders.add(providerConfig);
                
                SensorConsumerConfig consumerConfig = new SensorConsumerConfig();
                consumerConfig.enabled = true;
                consumerConfig.offering = offering;
                consumerConfig.sensorID = virtualSensor.getLocalID();
                config.dataConsumers.add(consumerConfig);
                
                if (config.newStorageConfig != null)
                {
                    // create new data storage
                    StorageConfig newStorageConfig = (StorageConfig)config.newStorageConfig.clone();
                    newStorageConfig.id = null;
                    newStorageConfig.name = virtualSensor.getName() + " Storage";
                    newStorageConfig.storagePath = sensorUID + ".dat";
                    IBasicStorage<?> storage = (IBasicStorage<?>)moduleReg.loadModule(newStorageConfig);
                    SensorStorageHelper dataListener = StorageHelper.configureStorageForSensor(virtualSensor, storage, true);
                                        
                    // associate storage to config                    
                    providerConfig.storageID = storage.getLocalID();
                    consumerConfig.storageID = storage.getLocalID();
                    
                    // save config so that registered sensor stays active after restart
                    moduleReg.saveConfiguration(this.config, sensorConfig, newStorageConfig, dataListener.getConfiguration());
                }
                
                // instantiate provider and consumer instances
                IDataProviderFactory provider = providerConfig.getFactory();
                ISOSDataConsumer consumer = consumerConfig.getConsumerInstance();
                
                // register provider and consumer
                dataProviders.put(offering, provider);
                dataConsumers.put(offering, consumer);
                
                // create new offering
                SOSOfferingCapabilities offCaps = provider.generateCapabilities();
                capabilitiesCache.getLayers().add(offCaps);
                offeringCaps.put(offCaps.getIdentifier(), offCaps);
                procedureToOfferingMap.put(sensorUID, offering);
            }
            
            // build and send response
            InsertSensorResponse resp = new InsertSensorResponse();
            resp.setAssignedOffering(offering);
            resp.setAssignedProcedureId(sensorUID);
            sendResponse(request, resp);
        }
        finally
        {
            
        }        
    }
    
    
    @Override
    protected boolean writeCustomFormatStream(GetResultRequest request, ISOSDataProvider dataProvider, OutputStream os) throws Exception
    {
        DataComponent resultStructure = dataProvider.getResultStructure();
        DataEncoding resultEncoding = dataProvider.getDefaultResultEncoding();
        
        if (resultEncoding instanceof BinaryEncoding)
        {
            boolean useMP4 = false;
            boolean useMJPEG = false;
            List<BinaryMember> mbrList = ((BinaryEncoding)resultEncoding).getMemberList();
            BinaryMember videoFrameSpec = null;
            
            if (mbrList.size() == 1) // case of no time tag
                videoFrameSpec = mbrList.get(0);
            else if (mbrList.size() == 2) // case of time tag + encoded frame
                videoFrameSpec = mbrList.get(1);
            else
                throw new RuntimeException("Invalid binary encoding specs");
            
            if (videoFrameSpec instanceof BinaryBlock && ((BinaryBlock)videoFrameSpec).getCompression().equals("H264"))
                useMP4 = true;            
            else if (videoFrameSpec instanceof BinaryBlock && ((BinaryBlock)videoFrameSpec).getCompression().equals("JPEG"))
                useMJPEG = true;            
            
            if (useMP4)
            {            
                // set MIME type for MP4 format
                if (request.getHttpResponse() != null)
                    request.getHttpResponse().setContentType("video/mp4");
                
                //os = new FileOutputStream("/home/alex/testsos.mp4");
                
                // TODO generate header on the fly using SWE Common structure
                String header = "00 00 00 20 66 74 79 70 69 73 6F 6D 00 00 02 00 69 73 6F 6D 69 73 6F 32 61 76 63 31 6D 70 34 31 00 00 02 E5 6D 6F 6F 76 00 00 00 6C 6D 76 68 64 00 00 00 00 D0 C3 54 92 D0 C3 54 92 00 00 03 E8 00 00 00 00 00 01 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 02 00 00 00 18 69 6F 64 73 00 00 00 00 10 80 80 80 07 00 4F FF FF FF FF FF 00 00 01 D1 74 72 61 6B 00 00 00 5C 74 6B 68 64 00 00 00 0F D0 C3 54 92 D0 C3 54 92 00 00 00 01 00 00 00 00 FF FF FF FF 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 40 00 00 00 07 80 00 00 04 38 00 00 00 00 01 6D 6D 64 69 61 00 00 00 20 6D 64 68 64 00 00 00 00 D0 C3 54 92 D0 C3 54 92 00 01 5F 90 FF FF FF FF 15 C7 00 00 00 00 00 2D 68 64 6C 72 00 00 00 00 00 00 00 00 76 69 64 65 00 00 00 00 00 00 00 00 00 00 00 00 56 69 64 65 6F 48 61 6E 64 6C 65 72 00 00 00 01 18 6D 69 6E 66 00 00 00 14 76 6D 68 64 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 24 64 69 6E 66 00 00 00 1C 64 72 65 66 00 00 00 00 00 00 00 01 00 00 00 0C 75 72 6C 20 00 00 00 01 00 00 00 D8 73 74 62 6C 00 00 00 8C 73 74 73 64 00 00 00 00 00 00 00 01 00 00 00 7C 61 76 63 31 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 07 80 04 38 00 48 00 00 00 48 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 18 FF FF 00 00 00 26 61 76 63 43 01 42 80 28 FF E1 00 0F 67 42 80 28 DA 01 E0 08 9F 96 01 B4 28 4D 40 01 00 04 68 CE 06 E2 00 00 00 10 73 74 74 73 00 00 00 00 00 00 00 00 00 00 00 10 73 74 73 63 00 00 00 00 00 00 00 00 00 00 00 14 73 74 73 7A 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 10 73 74 63 6F 00 00 00 00 00 00 00 00 00 00 00 28 6D 76 65 78 00 00 00 20 74 72 65 78 00 00 00 00 00 00 00 01 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 60 75 64 74 61 00 00 00 58 6D 65 74 61 00 00 00 00 00 00 00 21 68 64 6C 72 00 00 00 00 00 00 00 00 6D 64 69 72 61 70 70 6C 00 00 00 00 00 00 00 00 00 00 00 00 2B 69 6C 73 74 00 00 00 23 A9 74 6F 6F 00 00 00 1B 64 61 74 61 00 00 00 01 00 00 00 00 4C 61 76 66 35 34 2E 32 30 2E 34";
                for (String val: header.split(" ")) {
                    int b = Integer.parseInt(val, 16);
                    os.write(b);
                }
                
                // prepare record writer for selected encoding
                DataStreamWriter writer = SWEFactory.createDataWriter(resultEncoding);
                writer.setDataComponents(resultStructure);
                writer.setOutput(os);
                
                // write each record in output stream
                DataBlock nextRecord;
                while ((nextRecord = dataProvider.getNextResultRecord()) != null)
                {
                    writer.write(nextRecord);
                    writer.flush();
                }       
                        
                os.flush();
                return true;
            }
            
            else if (useMJPEG)
            {
                if (request.getHttpResponse() != null)
                {
                    request.getHttpResponse().addHeader("Cache-Control", "no-cache");
                    request.getHttpResponse().addHeader("Pragma", "no-cache");
                    
                    // set multi-part MIME so that browser can properly decode it in an img tag
                    //request.getHttpResponse().setContentType("image/jpeg"); //video/x-motion-jpeg, video/x-jpeg
                    request.getHttpResponse().setContentType("multipart/x-mixed-replace; boundary=--myboundary");
                }
                
                byte[] mimeBoundary = new String("--myboundary\r\nContent-Type: image/jpeg\r\nContent-Length: ").getBytes();
                byte[] endMime = new byte[] {0xD, 0xA, 0xD, 0xA};
                
                // write each record in output stream
                DataBlock nextRecord;
                while ((nextRecord = dataProvider.getNextResultRecord()) != null)
                {
                    DataBlock frameBlk = ((DataBlockMixed)nextRecord).getUnderlyingObject()[1];
                    byte[] frameData = (byte[])frameBlk.getUnderlyingObject();
                    
                    // write MIME boundary
                    os.write(mimeBoundary);
                    os.write(Integer.toString(frameData.length).getBytes());
                    os.write(endMime);
                    os.write(frameData);
                    os.flush();
                }       
                        
                os.flush();
                return true;
            }
        }
        
        return false;
    }
    
    
    @Override
    protected void handleRequest(DeleteSensorRequest request) throws Exception
    {
        try
        {
            checkTransactionalSupport(request);
            String sensorUID = request.getProcedureId();
            
            // check query parameters
            OWSExceptionReport report = new OWSExceptionReport();
            checkQueryProcedure(sensorUID, report);
            report.process();

            // destroy associated virtual sensor
            String offering = procedureToOfferingMap.get(sensorUID);
            SOSVirtualSensor virtualSensor = (SOSVirtualSensor)dataConsumers.remove(offering);
            procedureToOfferingMap.remove(sensorUID);
            SensorHub.getInstance().getModuleRegistry().destroyModule(virtualSensor.getLocalID());
            
            // TODO destroy storage if requested in config 
            
            // build and send response
            DeleteSensorResponse resp = new DeleteSensorResponse(SOSUtils.SOS);
            resp.setDeletedProcedure(sensorUID);
            sendResponse(request, resp);
        }
        finally
        {

        }
    }
    
    
    @Override
    protected void handleRequest(UpdateSensorRequest request) throws Exception
    {
        try
        {
            checkTransactionalSupport(request);
            String sensorUID = request.getProcedureId();
            
            // check query parameters
            OWSExceptionReport report = new OWSExceptionReport();
            checkQueryProcedure(sensorUID, report);
            checkQueryProcedureFormat(procedureToOfferingMap.get(sensorUID), request.getProcedureDescriptionFormat(), report);
            report.process();
            
            // get consumer and update
            ISOSDataConsumer consumer = getDataConsumerBySensorID(request.getProcedureId());                
            consumer.updateSensor(request.getProcedureDescription());
            
            // build and send response
            UpdateSensorResponse resp = new UpdateSensorResponse(SOSUtils.SOS);
            resp.setUpdatedProcedure(sensorUID);
            sendResponse(request, resp);
        }
        finally
        {

        }
    }
    
    
    @Override
    protected void handleRequest(InsertObservationRequest request) throws Exception
    {
        try
        {
            checkTransactionalSupport(request);
            
            ISOSDataConsumer consumer = getDataConsumerByOfferingID(request.getOffering());
            consumer.newObservation(request.getObservations().toArray(new IObservation[0]));            
            
            // build and send response
            InsertObservationResponse resp = new InsertObservationResponse();
            sendResponse(request, resp);
        }
        finally
        {
            
        }
    }
    
    
    @Override
    protected void handleRequest(InsertResultTemplateRequest request) throws Exception
    {
        try
        {
            checkTransactionalSupport(request);
            String offering = request.getOffering();
            
            // get template ID
            // the same template ID is always returned for a given observable
            ISOSDataConsumer consumer = getDataConsumerByOfferingID(offering);
            String templateID = consumer.newResultTemplate(request.getResultStructure(), request.getResultEncoding());
            
            // only continue of template was not already registered
            if (!templateToOfferingMap.containsKey(templateID))
            {
                templateToOfferingMap.put(templateID, offering);
                
                // re-generate capabilities
                IDataProviderFactory provider = getDataProviderFactoryByOfferingID(offering);
                SOSOfferingCapabilities newCaps = provider.generateCapabilities();
                int oldIndex = 0;
                for (OWSLayerCapabilities offCaps: capabilitiesCache.getLayers())
                {
                    if (offCaps.getIdentifier().equals(offering))
                        break; 
                    oldIndex++;
                }
                capabilitiesCache.getLayers().set(oldIndex, newCaps);
                offeringCaps.put(newCaps.getIdentifier(), newCaps);
            }
            
            // build and send response
            InsertResultTemplateResponse resp = new InsertResultTemplateResponse();
            resp.setAcceptedTemplateId(templateID);
            sendResponse(request, resp);
        }
        finally
        {
            
        }
    }
    
    
    @Override
    protected void handleRequest(InsertResultRequest request) throws Exception
    {
        DataStreamParser parser = null;
        
        checkTransactionalSupport(request);
        String templateID = request.getTemplateId();
        
        // retrieve consumer based on template id
        ISOSDataConsumer consumer = (ISOSDataConsumer)getDataConsumerByTemplateID(templateID);
        Template template = consumer.getTemplate(templateID);
        DataComponent dataStructure = template.component;
        DataEncoding encoding = template.encoding;
        
        try
        {
            InputStream resultStream;
            
            // select data source (either inline XML or in POST body for KVP)
            DataSource dataSrc = request.getResultDataSource();
            if (dataSrc instanceof DataSourceDOM) // inline XML
            {
                encoding = SWEFactory.ensureXmlCompatible(encoding);
                resultStream = dataSrc.getDataStream();
            }
            else // POST body
            {
                resultStream = new BufferedInputStream(request.getHttpRequest().getInputStream());
            }
            
            // create parser
            parser = SWEFactory.createDataParser(encoding);
            parser.setDataComponents(dataStructure);
            parser.setInput(resultStream);
                        
            // parse each record and send it to consumer
            DataBlock nextBlock = null;
            while ((nextBlock = parser.parseNextBlock()) != null)
                consumer.newResultRecord(templateID, nextBlock);
            
            // build and send response
            InsertResultResponse resp = new InsertResultResponse();
            sendResponse(request, resp);
        }
        catch (ReaderException e)
        {
            throw new SOSException("Error in SWE encoded data", e);
        }
        finally
        {
            if (parser != null)
                parser.close();
        }
    }


    @Override
    protected void checkQueryObservables(String offeringID, List<String> observables, OWSExceptionReport report) throws SOSException
    {
        SWESOfferingCapabilities offering = checkAndGetOffering(offeringID);
        for (String obsProp: observables)
        {
            if (!offering.getObservableProperties().contains(obsProp))
                report.add(new SOSException(SOSException.invalid_param_code, "observedProperty", obsProp, "Observed property " + obsProp + " is not available for offering " + offeringID));
        }
    }


    @Override
    protected void checkQueryProcedures(String offeringID, List<String> procedures, OWSExceptionReport report) throws SOSException
    {
        SWESOfferingCapabilities offering = checkAndGetOffering(offeringID);
        for (String procID: procedures)
        {
            if (!offering.getProcedures().contains(procID))
                report.add(new SOSException(SOSException.invalid_param_code, "procedure", procID, "Procedure " + procID + " is not available for offering " + offeringID));
        }
    }


    @Override
    protected void checkQueryFormat(String offeringID, String format, OWSExceptionReport report) throws SOSException
    {
        SOSOfferingCapabilities offering = checkAndGetOffering(offeringID);
        if (!offering.getResponseFormats().contains(format))
            report.add(new SOSException(SOSException.invalid_param_code, "format", format, "Format " + format + " is not available for offering " + offeringID));
    }


    @Override
    protected void checkQueryTime(String offeringID, TimeExtent requestTime, OWSExceptionReport report) throws SOSException
    {
        SOSOfferingCapabilities offering = checkAndGetOffering(offeringID);
        
        // refresh offering capabilities if needed
        try
        {
            IDataProviderFactory provider = (IDataProviderFactory)dataProviders.get(offeringID);
            provider.updateCapabilities();
        }
        catch (Exception e)
        {
            log.error("Error while updating capabilities for offering " + offeringID, e);
        }
        
        // check that request time is within allowed time period
        TimeExtent allowedPeriod = offering.getPhenomenonTime();
        boolean nowOk = allowedPeriod.isBaseAtNow() || allowedPeriod.isEndNow();
        
        boolean requestOk = false;
        if (requestTime.isBaseAtNow() && nowOk)
            requestOk = true;
        else if (requestTime.isBeginNow() && nowOk)
        {
            double now = System.currentTimeMillis() / 1000.0;
            if (requestTime.getStopTime() >= now)
                requestOk = true;
        }
        else if (requestTime.intersects(allowedPeriod))
            requestOk = true;
        
        if (!requestOk)
            report.add(new SOSException(SOSException.invalid_param_code, "phenomenonTime", requestTime.getIsoString(0), null));            
    }
    
    
    protected void checkQueryProcedure(String sensorUID, OWSExceptionReport report) throws SOSException
    {
        if (sensorUID == null || !procedureToOfferingMap.containsKey(sensorUID))
            report.add(new SOSException(SOSException.invalid_param_code, "procedure", sensorUID, null));
    }
    
    
    protected void checkQueryProcedureFormat(String offeringID, String format, OWSExceptionReport report) throws SOSException
    {
        // ok if default format can be used
        if (format == null)
            return;
        
        SWESOfferingCapabilities offering = offeringCaps.get(offeringID);
        if (offering != null)
        {
            if (!offering.getProcedureFormats().contains(format))
                report.add(new SOSException(SOSException.invalid_param_code, "procedureDescriptionFormat", format, "Procedure description format " + format + " is not available for offering " + offeringID));
        }
    }
    
    
    protected SOSOfferingCapabilities checkAndGetOffering(String offeringID) throws SOSException
    {
        SOSOfferingCapabilities offCaps = offeringCaps.get(offeringID);
        
        if (offCaps == null)
            throw new SOSException(SOSException.invalid_param_code, "offering", offeringID, null);
        
        return offCaps;
    }
    
    
    static String INVALID_SML_MSG = "Invalid SensorML description: ";
    protected void checkSensorML(AbstractProcess smlProcess, OWSExceptionReport report) throws Exception
    {
        String sensorUID = smlProcess.getUniqueIdentifier();
        
        if (sensorUID == null || sensorUID.length() == 0)
            throw new SOSException(SOSException.invalid_param_code, "procedureDescription", null, INVALID_SML_MSG + "Missing unique ID");
        
        if (sensorUID.length() < 10)
            report.add(new SOSException(SOSException.invalid_param_code, "procedureDescription", sensorUID, INVALID_SML_MSG + "Procedure unique ID is too short"));
        
        if (procedureToOfferingMap.containsKey(smlProcess.getIdentifier()))
            report.add(new SOSException(SOSException.invalid_param_code, "procedureDescription", sensorUID, INVALID_SML_MSG + "A procedure with unique ID " + sensorUID + " is already registered on this server"));
    }
    
    
    @Override
    protected ISOSDataProvider getDataProvider(String offering, SOSDataFilter filter) throws Exception
    {
        checkAndGetOffering(offering);
        return super.getDataProvider(offering, filter);
    }
    
    
    protected IDataProviderFactory getDataProviderFactoryByOfferingID(String offering) throws Exception
    {
        ISOSDataProviderFactory factory = dataProviders.get(offering);
        if (factory == null)
            throw new IllegalStateException("No valid data provider factory found for offering " + offering);
        return (IDataProviderFactory)factory;
    }
    
    
    protected IDataProviderFactory getDataProviderFactoryBySensorID(String sensorID) throws Exception
    {
        String offering = procedureToOfferingMap.get(sensorID);
        return getDataProviderFactoryByOfferingID(offering);
    }
    
    
    protected ISOSDataConsumer getDataConsumerByOfferingID(String offering) throws Exception
    {
        checkAndGetOffering(offering);
        ISOSDataConsumer consumer = dataConsumers.get(offering);
        
        if (consumer == null)
            throw new SOSException(SOSException.invalid_param_code, "offering", offering, "Transactional operations are not supported for offering " + offering);
            
        return consumer;
    }
    
    
    protected ISOSDataConsumer getDataConsumerBySensorID(String sensorID) throws Exception
    {
        String offering = procedureToOfferingMap.get(sensorID);
        
        if (offering == null)
            throw new SOSException(SOSException.invalid_param_code, "procedure", sensorID, "Transactional operations are not supported for sensor " + sensorID);
        
        return getDataConsumerByOfferingID(offering);
    }
    
    
    protected ISOSDataConsumer getDataConsumerByTemplateID(String templateID) throws Exception
    {
        String offering = templateToOfferingMap.get(templateID);
        ISOSDataConsumer consumer = dataConsumers.get(offering);
        
        if (consumer == null)
            throw new SOSException(SOSException.invalid_param_code, "template", templateID, "Invalid template ID");
        
        return consumer;
    }
    
    
    protected void checkTransactionalSupport(OWSRequest request) throws Exception
    {
        if (!config.enableTransactional)
            throw new SOSException(SOSException.invalid_param_code, "request", request.getOperation(), request.getOperation() + " operation is not supported on this endpoint"); 
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        // TODO Auto-generated method stub        
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        // TODO Auto-generated method stub        
    }


    @Override
    protected String getDefaultVersion()
    {
        return "2.0";
    }
}
