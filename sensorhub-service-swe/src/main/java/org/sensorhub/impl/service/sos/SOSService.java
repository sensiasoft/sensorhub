/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.service.IServiceModule;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.sensor.sost.SOSVirtualSensorConfig;
import org.sensorhub.impl.sensor.sost.SOSVirtualSensor;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.ogc.OGCServiceConfig.CapabilitiesInfo;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.cdm.common.DataEncoding;
import org.vast.cdm.common.DataStreamParser;
import org.vast.ogc.om.IObservation;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSExceptionReport;
import org.vast.ows.OWSRequest;
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataConsumer;
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
import org.vast.ows.swe.UpdateSensorRequest;
import org.vast.ows.swe.UpdateSensorResponse;
import org.vast.sensorML.SMLProcess;
import org.vast.sensorML.SMLUtils;
import org.vast.sensorML.system.SMLSystem;
import org.vast.sweCommon.SWEFactory;
import org.vast.util.TimeExtent;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;


/**
 * <p>
 * Implementation of SensorHub generic SOS service.
 * This service is automatically configured (mostly) from information obtained
 * from the selected data sources (sensors, storages, processes, etc).
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
@SuppressWarnings("serial")
public class SOSService extends SOSServlet implements IServiceModule<SOSServiceConfig>, IEventListener
{
    private static final Log log = LogFactory.getLog(SOSService.class);
    
    SOSServiceConfig config;
    SOSServiceCapabilities capabilitiesCache;
    Map<String, SOSOfferingCapabilities> offeringMap;
    Map<String, String> procedureToOfferingMap;
    Map<String, String> templateToOfferingMap;
    Map<String, ISOSDataConsumer> dataConsumers;

    
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
    public synchronized void updateConfig(SOSServiceConfig config) throws SensorHubException
    {
        // cleanup all previously instantiated providers        
        
        // rebuild everything

    }
    
    
    @Override
    public void start()
    {
        this.dataProviders.clear();
        this.dataConsumers = new LinkedHashMap<String, ISOSDataConsumer>();
        this.procedureToOfferingMap = new HashMap<String, String>();
        this.templateToOfferingMap = new HashMap<String, String>();
        this.offeringMap = new HashMap<String, SOSOfferingCapabilities>();
        
        // pre-generate capabilities
        this.capabilitiesCache = generateCapabilities();
                
        // subscribe to server lifecycle events
        SensorHub.getInstance().registerListener(this);
        
        // deploy service
        deploy();
    }
    
    
    @Override
    public void stop()
    {
        // undeploy ourself
        undeploy();
        
        // clean all providers
        for (ISOSDataProviderFactory provider: dataProviders.values())
            ((IDataProviderFactory)provider).cleanup();
    }
    
    
    /**
     * Generates the SOSCapabilities object with info from data source
     * @return
     * @throws ServiceException
     */
    protected SOSServiceCapabilities generateCapabilities()
    {
        procedureToOfferingMap.clear();
        
        // get main capabilities info from config
        CapabilitiesInfo serviceInfo = config.ogcCapabilitiesInfo;
        SOSServiceCapabilities capabilities = new SOSServiceCapabilities();
        capabilities.getIdentification().setTitle(serviceInfo.title);
        capabilities.getIdentification().setDescription(serviceInfo.description);
        capabilities.setFees(serviceInfo.fees);
        capabilities.setAccessConstraints(serviceInfo.accessConstraints);
        capabilities.setServiceProvider(serviceInfo.serviceProvider);
        
        // TODO generate profile list
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
                    IDataProviderFactory factory = providerConf.getFactory();
                    if (!factory.isEnabled())
                        continue;
                                    
                    dataProviders.put(providerConf.uri, factory);
         
                    // add offering metadata to capabilities
                    SOSOfferingCapabilities offCaps = factory.generateCapabilities();
                    capabilities.getLayers().add(offCaps);
                    offeringMap.put(offCaps.getIdentifier(), offCaps);
                    
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
                    SOSVirtualSensor virtualSensor = (SOSVirtualSensor)SensorHub.getInstance().getSensorManager().getModuleById(consumerConf.sensorID);
                    dataConsumers.put(consumerConf.offering, virtualSensor);
                }
                catch (SensorHubException e)
                {
                    log.error("Error while initializing virtual sensor " + consumerConf.sensorID, e);
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
    protected SMLSystem generateSensorML(String uri) throws ServiceException
    {
        try
        {
            IDataProviderFactory factory = getDataProviderFactoryBySensorID(uri);
            return (SMLSystem)factory.generateSensorMLDescription(null);
        }
        catch (Exception e)
        {
            throw new ServiceException("Error while retrieving SensorML description for sensor " + uri, e);
        }
    }
    
    
    @Override
    public synchronized SOSServiceConfig getConfiguration()
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


    @Override
    public void handleEvent(Event e)
    {
        // we need to deploy ourself when HTTP server is restarted
        if (e instanceof ModuleEvent && e.getSource() == HttpServer.getInstance())
        {
            if (((ModuleEvent) e).type == ModuleEvent.Type.ENABLED)
                start();
            else if (((ModuleEvent) e).type == ModuleEvent.Type.DISABLED)
                stop();
        }        
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {
        // cleanup all virtual sensors
        for (SOSConsumerConfig consumerConf: config.dataConsumers)
            SensorHub.getInstance().getModuleRegistry().destroyModule(consumerConf.sensorID);
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
    
    
    /////////////////////////////////////////
    /// methods overriden from SOSServlet ///
    /////////////////////////////////////////
    
    @Override
    protected void handleRequest(GetCapabilitiesRequest request) throws Exception
    {
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
        DOMHelper dom = new DOMHelper();
        SMLUtils smlUtils = new SMLUtils();
        Element respElt = smlUtils.writeSystem(dom, generateSensorML(sensorID));
        dom.serialize(respElt, request.getResponseStream(), null);
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
            String sensorUID = request.getProcedureDescription().getIdentifier();
            String offering = sensorUID + "-sos";
            
            // automatically add outputs with specified observable properties??
            
            
            // create and register new virtual sensor module
            SOSVirtualSensorConfig sensorConfig = new SOSVirtualSensorConfig();
            sensorConfig.enabled = true;
            sensorConfig.sensorUID = sensorUID;
            sensorConfig.name = request.getProcedureDescription().getName();
            SensorHub.getInstance().getPersistenceManager().getSensorDescriptionStorage().store(request.getProcedureDescription());
            SOSVirtualSensor virtualSensor = (SOSVirtualSensor)SensorHub.getInstance().getModuleRegistry().loadModule(sensorConfig);            
            dataConsumers.put(offering, virtualSensor);
            
            // add to SOS config
            SOSConsumerConfig consumerConfig = new SOSConsumerConfig();
            consumerConfig.offering = offering;
            consumerConfig.sensorID = virtualSensor.getLocalID();
            config.dataConsumers.add(consumerConfig);
            
            // create new sensor provider
            SensorDataProviderConfig providerConfig = new SensorDataProviderConfig();
            providerConfig.sensorID = virtualSensor.getLocalID();
            providerConfig.enabled = true;
            providerConfig.uri = offering;
            SensorDataProviderFactory provider = new SensorDataProviderFactory(providerConfig);
            dataProviders.put(offering, provider);
            config.dataProviders.add(providerConfig);
            
            // create new offering
            SOSOfferingCapabilities offCaps = provider.generateCapabilities();
            capabilitiesCache.getLayers().add(offCaps);
            offeringMap.put(offCaps.getIdentifier(), offCaps);
            procedureToOfferingMap.put(sensorUID, offering);
            
            // setup data storage
            //StorageConfig storageConfig = new StorageConfig();
            //SensorHub.getInstance().getModuleRegistry().loadModule(storageConfig);
            
            // save config so that registered sensor stays active after restart
            SensorHub.getInstance().getModuleRegistry().saveConfiguration(this);
            
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
            consumer.updateSensor((SMLProcess) request.getProcedureDescription());
            
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
            
            ISOSDataConsumer consumer = getDataConsumerByOfferingID(request.getOffering());
            String templateID = consumer.newResultTemplate(request.getResultStructure(), request.getResultEncoding());
            
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
        
        try
        {
            checkTransactionalSupport(request);
            String templateID = request.getTemplateId();
            
            // retrieve consumer based on template id
            SOSVirtualSensor sensor = (SOSVirtualSensor)getDataConsumerByTemplateID(templateID);
            ISensorDataInterface output = sensor.getObservationOutputs().get(templateID);
            DataComponent dataStructure = output.getRecordDescription();
            DataEncoding encoding = output.getRecommendedEncoding();
            
            // prepare parser
            parser = SWEFactory.createDataParser(encoding);
            parser.setDataComponents(dataStructure);
            parser.setInput(request.getResultDataSource().getDataStream());
            
            // parse each record and send it to consumer
            DataBlock nextBlock = null;
            while ((nextBlock = parser.parseNextBlock()) != null)
                sensor.newResultRecord(templateID, nextBlock);
            
            // build and send response
            InsertResultResponse resp = new InsertResultResponse();
            sendResponse(request, resp);
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
        SOSOfferingCapabilities offering = checkAndGetOffering(offeringID);
        for (String obsProp: observables)
        {
            if (!offering.getObservableProperties().contains(obsProp))
                report.add(new SOSException(SOSException.invalid_param_code, "observedProperty", obsProp, "Observed property " + obsProp + " is not available for offering " + offeringID));
        }
    }


    @Override
    protected void checkQueryProcedures(String offeringID, List<String> procedures, OWSExceptionReport report) throws SOSException
    {
        SOSOfferingCapabilities offering = checkAndGetOffering(offeringID);
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
        
        boolean ok = false;
        for (TimeExtent timeRange: offering.getPhenomenonTimes())
        {
            if (timeRange.contains(requestTime))
            {
                ok = true;
                break;
            }            
        }
        
        if (!ok)
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
        
        SOSOfferingCapabilities offering = checkAndGetOffering(offeringID);
        if (!offering.getProcedureFormats().contains(format))
            report.add(new SOSException(SOSException.invalid_param_code, "procedureDescriptionFormat", format, "Procedure description format " + format + " is not available for offering " + offeringID));
    }
    
    
    protected SOSOfferingCapabilities checkAndGetOffering(String offeringID) throws SOSException
    {
        SOSOfferingCapabilities offCaps = offeringMap.get(offeringID);
        
        if (offCaps == null)
            throw new SOSException(SOSException.invalid_param_code, "offering", offeringID, null);
        
        return offCaps;
    }
    
    
    static String INVALID_SML_MSG = "Invalid SensorML description: ";
    protected void checkSensorML(SMLProcess smlProcess, OWSExceptionReport report) throws Exception
    {
        String sensorUID = smlProcess.getIdentifier();
        
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
    
    
    protected IDataProviderFactory getDataProviderFactoryBySensorID(String sensorID) throws Exception
    {
        String offering = procedureToOfferingMap.get(sensorID);
        ISOSDataProviderFactory factory = dataProviders.get(offering);
        if (factory == null)
            throw new IllegalStateException("No valid data provider factory found for offering " + offering);
        return (IDataProviderFactory)factory;
    }
    
    
    protected ISOSDataConsumer getDataConsumerByOfferingID(String offering) throws Exception
    {
        checkAndGetOffering(offering);
        return dataConsumers.get(offering);
    }
    
    
    protected ISOSDataConsumer getDataConsumerBySensorID(String sensorID) throws Exception
    {
        String offering = procedureToOfferingMap.get(sensorID);
        return getDataConsumerByOfferingID(offering);
    }
    
    
    protected ISOSDataConsumer getDataConsumerByTemplateID(String templateID) throws Exception
    {
        String offering = templateToOfferingMap.get(templateID);
        return dataConsumers.get(offering);
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
}
