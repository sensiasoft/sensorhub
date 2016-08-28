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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import net.opengis.fes.v20.Conformance;
import net.opengis.fes.v20.FilterCapabilities;
import net.opengis.fes.v20.SpatialCapabilities;
import net.opengis.fes.v20.SpatialOperator;
import net.opengis.fes.v20.SpatialOperatorName;
import net.opengis.fes.v20.TemporalCapabilities;
import net.opengis.fes.v20.TemporalOperator;
import net.opengis.fes.v20.TemporalOperatorName;
import net.opengis.fes.v20.impl.FESFactory;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.BinaryMember;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.JSONEncoding;
import net.opengis.swe.v20.SimpleComponent;
import net.opengis.swe.v20.TextEncoding;
import net.opengis.swe.v20.Vector;
import net.opengis.swe.v20.XMLEncoding;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.FoiFilter;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.persistence.StreamStorageConfig;
import org.sensorhub.impl.sensor.sost.SOSVirtualSensorConfig;
import org.sensorhub.impl.sensor.sost.SOSVirtualSensor;
import org.sensorhub.impl.service.ogc.OGCServiceConfig.CapabilitiesInfo;
import org.sensorhub.impl.service.sos.ISOSDataConsumer.Template;
import org.sensorhub.utils.FileUtils;
import org.slf4j.Logger;
import org.vast.cdm.common.DataSource;
import org.vast.cdm.common.DataStreamParser;
import org.vast.cdm.common.DataStreamWriter;
import org.vast.ogc.OGCRegistry;
import org.vast.ogc.def.DefinitionRef;
import org.vast.ogc.gml.GMLStaxBindings;
import org.vast.ogc.gml.GenericFeature;
import org.vast.ogc.om.IObservation;
import org.vast.ogc.om.OMUtils;
import org.vast.ogc.om.SamplingPoint;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSExceptionReport;
import org.vast.ows.OWSLayerCapabilities;
import org.vast.ows.OWSRequest;
import org.vast.ows.OWSUtils;
import org.vast.ows.sos.GetFeatureOfInterestRequest;
import org.vast.ows.sos.GetResultRequest;
import org.vast.ows.sos.DataStructFilter;
import org.vast.ows.sos.GetObservationRequest;
import org.vast.ows.sos.GetResultTemplateRequest;
import org.vast.ows.sos.GetResultTemplateResponse;
import org.vast.ows.sos.InsertObservationRequest;
import org.vast.ows.sos.InsertObservationResponse;
import org.vast.ows.sos.InsertResultRequest;
import org.vast.ows.sos.InsertResultResponse;
import org.vast.ows.sos.InsertResultTemplateRequest;
import org.vast.ows.sos.InsertResultTemplateResponse;
import org.vast.ows.sos.InsertSensorRequest;
import org.vast.ows.sos.InsertSensorResponse;
import org.vast.ows.sos.SOSException;
import org.vast.ows.sos.SOSInsertionCapabilities;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.ows.sos.SOSServiceCapabilities;
import org.vast.ows.sos.SOSUtils;
import org.vast.ows.swe.DeleteSensorRequest;
import org.vast.ows.swe.DeleteSensorResponse;
import org.vast.ows.swe.DescribeSensorRequest;
import org.vast.ows.swe.SWESOfferingCapabilities;
import org.vast.ows.swe.UpdateSensorRequest;
import org.vast.ows.swe.UpdateSensorResponse;
import org.vast.sensorML.SMLStaxBindings;
import org.vast.swe.AbstractDataWriter;
import org.vast.swe.DataSourceDOM;
import org.vast.swe.FilteredWriter;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.util.ReaderException;
import org.vast.util.TimeExtent;
import org.vast.xml.DOMHelper;
import org.vast.xml.IXMLWriterDOM;
import org.vast.xml.IndentingXMLStreamWriter;
import org.vast.xml.XMLImplFinder;
import org.w3c.dom.Element;
import com.vividsolutions.jts.geom.Polygon;


/**
 * <p>
 * Extension of SOSServlet deployed as a SensorHub service
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
@SuppressWarnings("serial")
public class SOSServlet extends org.vast.ows.sos.SOSServlet
{
    private static final String INVALID_WS_REQ_MSG = "Invalid WebSocket request: ";
    private static final String INVALID_SML_MSG = "Invalid SensorML description: ";
        
    private static final QName EXT_REPLAY = new QName("replayspeed"); // kvp params are always lower case
    
    SOSServiceConfig config;
    SOSSecurity securityHandler;
    Logger log;
    String endpointUrl;
    ReentrantReadWriteLock capabilitiesLock = new ReentrantReadWriteLock();
    SOSServiceCapabilities capabilities;
    Map<String, SOSOfferingCapabilities> offeringCaps;
    Map<String, String> procedureToOfferingMap;
    Map<String, String> templateToOfferingMap;    
    Map<String, ISOSDataProviderFactory> dataProviders = new LinkedHashMap<String, ISOSDataProviderFactory>();
    Map<String, ISOSDataConsumer> dataConsumers;
    Map<String, ISOSCustomSerializer> customFormats = new HashMap<String, ISOSCustomSerializer>();
    boolean needCapabilitiesTimeUpdate = false;
    SOSSecurity sosSecurity;
    
    
    protected SOSServlet(SOSServiceConfig config, SOSSecurity securityHandler, Logger log)
    {
        this.config = config;
        this.securityHandler = securityHandler;
        this.log = log;
    }
    
    
    protected void start() throws SensorHubException
    {
        this.dataConsumers = new LinkedHashMap<String, ISOSDataConsumer>();
        this.procedureToOfferingMap = new HashMap<String, String>();
        this.templateToOfferingMap = new HashMap<String, String>();
        this.offeringCaps = new HashMap<String, SOSOfferingCapabilities>();
        
        // pre-generate capabilities
        endpointUrl = null;
        generateCapabilities();
    }
    
    
    protected void stop()
    {
        // clean all providers
        for (ISOSDataProviderFactory provider: dataProviders.values())
            ((ISOSDataProviderFactory)provider).cleanup();
    }
    
    
    /**
     * Generates the SOSServiceCapabilities object with info from data source
     */
    protected void generateCapabilities() throws SensorHubException
    {
        dataProviders.clear();
        procedureToOfferingMap.clear();
        templateToOfferingMap.clear();
        offeringCaps.clear();
        capabilities = new SOSServiceCapabilities();
        
        // get main capabilities info from config
        CapabilitiesInfo serviceInfo = config.ogcCapabilitiesInfo;
        capabilities.getSupportedVersions().add(DEFAULT_VERSION);
        capabilities.getIdentification().setTitle(serviceInfo.title);
        capabilities.getIdentification().setDescription(serviceInfo.description);
        capabilities.setFees(serviceInfo.fees);
        capabilities.setAccessConstraints(serviceInfo.accessConstraints);
        capabilities.setServiceProvider(serviceInfo.serviceProvider);
        
        // supported operations and extensions
        capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_RESULT_RETRIEVAL);
        capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_OMXML);
        capabilities.getGetServers().put("GetCapabilities", config.endPoint);
        capabilities.getGetServers().put("DescribeSensor", config.endPoint);
        capabilities.getGetServers().put("GetFeatureOfInterest", config.endPoint);
        capabilities.getGetServers().put("GetObservation", config.endPoint);
        capabilities.getGetServers().put("GetResult", config.endPoint);
        capabilities.getGetServers().put("GetResultTemplate", config.endPoint);
        capabilities.getPostServers().putAll(capabilities.getGetServers());
        
        if (config.enableTransactional)
        {
            capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_SENSOR_INSERTION);
            capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_SENSOR_DELETION);
            capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_OBS_INSERTION);
            capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_RESULT_INSERTION);
            capabilities.getPostServers().put("InsertSensor", config.endPoint);
            capabilities.getPostServers().put("DeleteSensor", config.endPoint);
            capabilities.getPostServers().put("InsertObservation", config.endPoint);
            capabilities.getPostServers().put("InsertResultTemplate", config.endPoint);
            capabilities.getPostServers().put("InsertResult", config.endPoint);
            capabilities.getGetServers().put("InsertResult", config.endPoint);
            
            // insertion capabilities
            SOSInsertionCapabilities insertCaps = new SOSInsertionCapabilities();
            insertCaps.getProcedureFormats().add(SWESOfferingCapabilities.FORMAT_SML2);
            insertCaps.getFoiTypes().add(SamplingPoint.TYPE);
            insertCaps.getObservationTypes().add(IObservation.OBS_TYPE_SCALAR);
            insertCaps.getObservationTypes().add(IObservation.OBS_TYPE_RECORD);
            insertCaps.getObservationTypes().add(IObservation.OBS_TYPE_ARRAY);
            insertCaps.getSupportedEncodings().add(SOSServiceCapabilities.SWE_ENCODING_TEXT);
            insertCaps.getSupportedEncodings().add(SOSServiceCapabilities.SWE_ENCODING_BINARY);
            capabilities.setInsertionCapabilities(insertCaps);
        }
        
        // filter capabilities
        FESFactory fac = new FESFactory();
        FilterCapabilities filterCaps = fac.newFilterCapabilities();
        capabilities.setFilterCapabilities(filterCaps);
        
        // conformance
        Conformance filterConform = filterCaps.getConformance();
        filterConform.addConstraint(fac.newConstraint("ImplementsQuery", Boolean.TRUE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsAdHocQuery", Boolean.FALSE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsFunctions", Boolean.FALSE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsResourceld", Boolean.FALSE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsMinStandardFilter", Boolean.FALSE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsStandardFilter", Boolean.FALSE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsMinSpatialFilter", Boolean.TRUE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsSpatialFilter", Boolean.FALSE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsMinTemporalFilter", Boolean.TRUE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsTemporalFilter", Boolean.FALSE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsVersionNav", Boolean.FALSE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsSorting", Boolean.FALSE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsExtendedOperators", Boolean.FALSE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsMinimumXPath", Boolean.FALSE.toString()));
        filterConform.addConstraint(fac.newConstraint("ImplementsSchemaElementFunc", Boolean.FALSE.toString()));
        
        // supported temporal filters
        TemporalCapabilities timeFilterCaps = fac.newTemporalCapabilities();
        timeFilterCaps.getTemporalOperands().add(new QName(null, "TimeInstant", "gml"));
        timeFilterCaps.getTemporalOperands().add(new QName(null, "TimePeriod", "gml"));
        TemporalOperator timeOp = fac.newTemporalOperator();
        timeOp.setName(TemporalOperatorName.DURING);
        timeFilterCaps.getTemporalOperators().add(timeOp);
        filterCaps.setTemporalCapabilities(timeFilterCaps);
        
        // supported spatial filters
        SpatialCapabilities spatialFilterCaps = fac.newSpatialCapabilities();
        spatialFilterCaps.getGeometryOperands().add(new QName(null, "Envelope", "gml"));
        SpatialOperator spatialOp = fac.newSpatialOperator();
        spatialOp.setName(SpatialOperatorName.BBOX);
        spatialFilterCaps.getSpatialOperators().add(spatialOp);
        filterCaps.setSpatialCapabilities(spatialFilterCaps);
        
        // process each provider config
        if (config.dataProviders != null)
        {
            for (SOSProviderConfig providerConf: config.dataProviders)
            {
                try
                {
                    // instantiate provider factories and map them to offering URIs
                    ISOSDataProviderFactory provider = providerConf.getFactory(this);
                    dataProviders.put(providerConf.uri, provider);
                    
                    // create offering only if not already done when registering
                    // the provider factory listener (if data source was in STARTED
                    // state it can automatically trigger a call to showProviderCaps).
                    if (!offeringCaps.containsKey(providerConf.uri) && provider.isEnabled())
                        showProviderCaps(provider);
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
    }
    
    
    protected void showProviderCaps(ISOSDataProviderFactory provider)
    {
        SOSProviderConfig config = provider.getConfig();
                
        try
        {
            capabilitiesLock.writeLock().lock();
            
            // generate offering metadata
            SOSOfferingCapabilities offCaps = provider.generateCapabilities();
            String procedureID = offCaps.getMainProcedure();
            
            // update offering if it was already advertised
            if (offeringCaps.containsKey(config.uri))
            {
                // replace old offering
                SOSOfferingCapabilities oldCaps = offeringCaps.put(config.uri, offCaps);
                capabilities.getLayers().set(capabilities.getLayers().indexOf(oldCaps), offCaps);
                
                if (log.isDebugEnabled())
                    log.debug("Offering " + "\"" + offCaps.getIdentifier() + "\" updated for procedure " + procedureID);
            }
            
            // otherwise add new offering
            else
            {
                // add to maps and layer list
                offeringCaps.put(offCaps.getIdentifier(), offCaps);                
                procedureToOfferingMap.put(procedureID, offCaps.getIdentifier());                
                capabilities.getLayers().add(offCaps);
                
                if (log.isDebugEnabled())
                    log.debug("Offering " + "\"" + offCaps.getIdentifier() + "\" added for procedure " + procedureID);
            }
        }
        catch (Exception e)
        {
            log.error("Error while generating offering " + config.uri, e);
        }
        finally
        {
            capabilitiesLock.writeLock().unlock();
        }
    }
    
    
    protected void hideProviderCaps(ISOSDataProviderFactory provider)
    {
        SOSProviderConfig config = provider.getConfig();
        
        try
        {
            capabilitiesLock.writeLock().lock();
            
            // stop here if provider is not advertised
            if (!offeringCaps.containsKey(config.uri))
                return;
            
            // remove offering from capabilities
            SOSOfferingCapabilities offCaps = offeringCaps.remove(config.uri);
            capabilities.getLayers().remove(offCaps);
            
            // remove from procedure map
            String procedureID = offCaps.getMainProcedure();
            procedureToOfferingMap.remove(procedureID);
            
            if (log.isDebugEnabled())
                log.debug("Offering " + "\"" + offCaps.getIdentifier() + "\" removed for procedure " + procedureID);
        }
        finally
        {
            capabilitiesLock.writeLock().unlock();
        }
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
            ISOSDataProviderFactory factory = getDataProviderFactoryBySensorID(uri);
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
    
    
    private WebSocketServletFactory factory = new WebSocketServerFactory();
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // set current user name
        securityHandler.setCurrentUser(req.getRemoteUser());
        
        try
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
                            String errorMsg = INVALID_WS_REQ_MSG + owsReq.getOperation() + " is not supported via this protocol.";
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
        finally
        {
            securityHandler.clearCurrentUser();
        }
    }


    @Override
    protected void handleRequest(GetCapabilitiesRequest request) throws Exception
    {
        // check that version 2.0.0 is supported by client
        if (!request.getAcceptedVersions().isEmpty())
        {
            if (!request.getAcceptedVersions().contains(DEFAULT_VERSION))
                throw new SOSException(SOSException.version_nego_failed_code, "AcceptVersions", null,
                        "Only version " + DEFAULT_VERSION + " is supported by this server");
        }
        
        // set selected version
        request.setVersion(DEFAULT_VERSION);
        
        // security check
        securityHandler.check(securityHandler.sos_read_caps);
        
        // make sure capabilities are up to date
        try
        {
            capabilitiesLock.writeLock().lock();
            
            // update operation URLs
            if (endpointUrl == null)
            {
                endpointUrl = request.getHttpRequest().getRequestURL().toString();
                for (Entry<String, String> op: capabilities.getGetServers().entrySet())
                    capabilities.getGetServers().put(op.getKey(), endpointUrl);
                for (Entry<String, String> op: capabilities.getPostServers().entrySet())
                    capabilities.getPostServers().put(op.getKey(), endpointUrl);
            }
            
            // ask providers to refresh their capabilities if needed.
            // we do that here so capabilities doc contains the most up-to-date info.
            // we don't always do it when changes occur because high frequency changes 
            // would trigger too many updates (e.g. new measurements changing time periods)
            for (ISOSDataProviderFactory provider: dataProviders.values())
            {
                if (provider.isEnabled())
                    ((ISOSDataProviderFactory)provider).updateCapabilities();
            }
        }
        finally
        {
            capabilitiesLock.writeLock().unlock();
        }
        
        try
        {
            capabilitiesLock.readLock().lock();
            sendResponse(request, capabilities);
        }
        finally
        {
            capabilitiesLock.readLock().unlock();
        }
    }
        
    
    @Override
    protected void handleRequest(DescribeSensorRequest request) throws Exception
    {
        String sensorID = request.getProcedureID();
                
        // security check
        securityHandler.check(securityHandler.sos_read_sensor);
        
        // check query parameters
        OWSExceptionReport report = new OWSExceptionReport();        
        checkQueryProcedure(sensorID, report);
        checkQueryProcedureFormat(procedureToOfferingMap.get(sensorID), request.getFormat(), report);
        report.process();
        
        // get procedure description
        AbstractProcess processDesc = generateSensorML(sensorID, request.getTime());
        if (processDesc == null)
            throw new SOSException(SOSException.invalid_param_code, "validTime"); 
        
        // init xml document writing
        OutputStream os = new BufferedOutputStream(request.getResponseStream());
        XMLOutputFactory factory = XMLImplFinder.getStaxOutputFactory();
        XMLStreamWriter xmlWriter = factory.createXMLStreamWriter(os, StandardCharsets.UTF_8.name());
        xmlWriter = new IndentingXMLStreamWriter(xmlWriter);
        
        // prepare SensorML writing
        SMLStaxBindings smlBindings = new SMLStaxBindings();
        smlBindings.setNamespacePrefixes(xmlWriter);
        smlBindings.declareNamespacesOnRootElement();
        
        // start XML response
        xmlWriter.writeStartDocument();
        
        // wrap with SOAP envelope if requested
        String soapUri = request.getSoapVersion(); 
        if (soapUri != null)
        {
            xmlWriter.writeStartElement(SOAP_PREFIX, "Envelope", soapUri);
            xmlWriter.writeNamespace(SOAP_PREFIX, soapUri);
            xmlWriter.writeStartElement(SOAP_PREFIX, "Body", soapUri);
        }
        
        String swesNsUri = OGCRegistry.getNamespaceURI(SOSUtils.SWES, DEFAULT_VERSION);
        xmlWriter.writeStartElement(SWES_PREFIX, "DescribeSensorResponse", swesNsUri);
        xmlWriter.writeNamespace(SWES_PREFIX, swesNsUri);
        
        xmlWriter.writeStartElement(SWES_PREFIX, "procedureDescriptionFormat", swesNsUri);
        xmlWriter.writeCharacters(DescribeSensorRequest.DEFAULT_FORMAT);
        xmlWriter.writeEndElement();
        
        xmlWriter.writeStartElement(SWES_PREFIX, "description", swesNsUri);
        xmlWriter.writeStartElement(SWES_PREFIX, "SensorDescription", swesNsUri);
        xmlWriter.writeStartElement(SWES_PREFIX, "data", swesNsUri);
        smlBindings.writeAbstractProcess(xmlWriter, processDesc);
        xmlWriter.writeEndElement();
        xmlWriter.writeEndElement();
        xmlWriter.writeEndElement();
        
        xmlWriter.writeEndElement();
        
        // close SOAP elements
        if (soapUri != null)
        {
            xmlWriter.writeEndElement();
            xmlWriter.writeEndElement();
        }
        
        xmlWriter.writeEndDocument();
        xmlWriter.close();
    }
    
    
    @Override
    protected void handleRequest(GetObservationRequest request) throws Exception
    {
        ISOSDataProvider dataProvider = null;
        
        try
        {
            // set default format
            if (request.getFormat() == null)
                request.setFormat(GetObservationRequest.DEFAULT_FORMAT);
            
            // build offering set (also from procedures ID)
            Set<String> selectedOfferings = new HashSet<String>();
            for (String procID: request.getProcedures())
            {
                String offering = procedureToOfferingMap.get(procID);
                if (offering != null)
                    selectedOfferings.add(offering);                
            }
            if (selectedOfferings.isEmpty())
                selectedOfferings.addAll(request.getOfferings());
            else if (!request.getOfferings().isEmpty())
                selectedOfferings.retainAll(request.getOfferings());
            
            // if no offering or procedure specified scan all offerings
            if (selectedOfferings.isEmpty())
                selectedOfferings.addAll(offeringCaps.keySet());
            
            // check query parameters
            OWSExceptionReport report = new OWSExceptionReport();
            checkQueryOfferings(request.getOfferings(), report);
            checkQueryObservables(request.getObservables(), report);
            checkQueryProcedures(request.getProcedures(), report);
            for (String offering: selectedOfferings)
                checkQueryFormat(offering, request.getFormat(), report);
            report.process();
            
            // security check
            for (String offering: selectedOfferings)
                securityHandler.check(offering, securityHandler.sos_read_obs);
            
            // prepare obs stream writer for requested O&M version
            String format = request.getFormat();
            String omVersion = format.substring(format.lastIndexOf('/') + 1);
            IXMLWriterDOM<IObservation> obsWriter = (IXMLWriterDOM<IObservation>)OGCRegistry.createWriter(OMUtils.OM, OMUtils.OBSERVATION, omVersion);
            String sosNsUri = OGCRegistry.getNamespaceURI(SOSUtils.SOS, DEFAULT_VERSION);
            
            // init xml document writing
            OutputStream os = new BufferedOutputStream(request.getResponseStream());
            XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(os, "UTF-8");
            xmlWriter = new IndentingXMLStreamWriter(xmlWriter);
            
            // wrap with SOAP envelope if needed
            xmlWriter.writeStartDocument();
            startSoapEnvelope(request, xmlWriter);
            xmlWriter.writeStartElement(SOS_PREFIX, "GetObservationResponse", sosNsUri);
            xmlWriter.writeNamespace(SOS_PREFIX, sosNsUri);
            
            // send obs from each selected offering
            // TODO sort by time by multiplexing obs from different offerings?
            boolean firstObs = true;
            for (String offering: selectedOfferings)
            {
                List<String> selectedObservables = request.getObservables();
                                
                // if no observables were selected, add all of them
                // we'll filter redundant one later
                boolean sendAllObservables = false;
                if (selectedObservables.isEmpty())
                {
                   SOSOfferingCapabilities caps = offeringCaps.get(offering);
                   selectedObservables = new ArrayList<String>();
                   selectedObservables.addAll(caps.getObservableProperties());
                   sendAllObservables = true;
                }
                
                // setup data provider
                SOSDataFilter filter = new SOSDataFilter(request.getFoiIDs(), selectedObservables, request.getTime());
                filter.setMaxObsCount(config.maxObsCount);
                dataProvider = getDataProvider(offering, filter);
                
                // write each observation in stream
                // we use stream writer to limit memory usage
                IObservation obs;
                while ((obs = dataProvider.getNextObservation()) != null)
                {
                    DataComponent obsResult = obs.getResult();
                    
                    // write a different obs for each requested observable
                    for (String observable: selectedObservables)
                    {                    
                        obs.setObservedProperty(new DefinitionRef(observable));
                        
                        // filter obs result
                        if (!observable.equals(obsResult.getDefinition()))
                        {
                            DataComponent singleResult = SWEHelper.findComponentByDefinition(obsResult, observable);
                            obs.setResult(singleResult);
                        }
                        else
                        {
                            // make sure we reset the whole result in case it was trimmed during previous iteration
                            obs.setResult(obsResult);
                        }
                        
                        // remove redundant obs in wildcard case
                        DataComponent result = obs.getResult();
                        if (sendAllObservables)
                        {
                            if (result instanceof DataRecord || result instanceof DataChoice)
                                continue;
                        }
                        
                        // set correct obs type depending on final result structure                        
                        if (result instanceof SimpleComponent)
                            obs.setType(IObservation.OBS_TYPE_SCALAR);
                        else if (result instanceof DataRecord || result instanceof Vector)
                            obs.setType(IObservation.OBS_TYPE_RECORD);
                        else if (result instanceof DataArray)
                            obs.setType(IObservation.OBS_TYPE_ARRAY);
                        
                        // first write obs as DOM
                        DOMHelper dom = new DOMHelper();
                        Element obsElt = obsWriter.write(dom, obs);
                        
                        // write common namespaces on root element
                        if (firstObs)
                        {
                            for (Entry<String, String> nsDef: dom.getXmlDocument().getNSTable().entrySet())
                                xmlWriter.writeNamespace(nsDef.getKey(), nsDef.getValue());        
                            firstObs = false;
                        }
                        
                        // serialize observation DOM tree into stream writer
                        xmlWriter.writeStartElement(SOS_PREFIX, "observationData", sosNsUri);
                        dom.writeToStreamWriter(obsElt, xmlWriter);
                        xmlWriter.writeEndElement();
                        xmlWriter.flush();
                    }
                }
            }
            
            // this will automatically close all open elements
            xmlWriter.writeEndDocument();
            xmlWriter.close();
        }
        finally
        {
            if (dataProvider != null)
                dataProvider.close();
        }
    }
    
    
    protected void handleRequest(GetResultTemplateRequest request) throws Exception
    {
        ISOSDataProvider dataProvider = null;
        
        try
        {
            // check query parameters        
            OWSExceptionReport report = new OWSExceptionReport();
            checkQueryObservables(request.getOffering(), request.getObservables(), report);
            report.process();
            
            // security check
            securityHandler.check(request.getOffering(), securityHandler.sos_read_obs);
            
            // setup data provider
            SOSDataFilter filter = new SOSDataFilter(request.getObservables().get(0));
            dataProvider = getDataProvider(request.getOffering(), filter);
            
            // build filtered component tree
            DataComponent filteredStruct = dataProvider.getResultStructure().copy();
            request.getObservables().add(SWEConstants.DEF_SAMPLING_TIME); // always keep sampling time
            filteredStruct.accept(new DataStructFilter(request.getObservables()));
            
            // build and send response 
            GetResultTemplateResponse resp = new GetResultTemplateResponse();
            resp.setResultStructure(filteredStruct);
            resp.setResultEncoding(dataProvider.getDefaultResultEncoding());
            sendResponse(request, resp);            
        }
        finally
        {
            if (dataProvider != null)
                dataProvider.close();
        }
    }
    
    
    protected void handleRequest(GetResultRequest request) throws Exception
    {
        ISOSDataProvider dataProvider = null;
        
        try
        {
            // check query parameters
            OWSExceptionReport report = new OWSExceptionReport();
            checkQueryObservables(request.getOffering(), request.getObservables(), report);
            checkQueryProcedures(request.getOffering(), request.getProcedures(), report);
            checkQueryTime(request.getOffering(), request.getTime(), report);
            report.process();
            
            // security check
            securityHandler.check(request.getOffering(), securityHandler.sos_read_obs);
            
            // setup data filter (including extensions)
            SOSDataFilter filter = new SOSDataFilter(request.getFoiIDs(), request.getObservables(), request.getTime());
            filter.setMaxObsCount(config.maxRecordCount);
            if (request.getExtensions().containsKey(EXT_REPLAY))
            {
                String replaySpeed = (String)request.getExtensions().get(EXT_REPLAY);
                filter.setReplaySpeedFactor(Double.parseDouble(replaySpeed));
            }
            
            // setup data provider
            dataProvider = getDataProvider(request.getOffering(), filter);
            DataComponent resultStructure = dataProvider.getResultStructure();
            DataEncoding resultEncoding = dataProvider.getDefaultResultEncoding();
            
            // use specific format handler if available
            boolean dataWritten = writeCustomFormatStream(request, dataProvider);
            
            // otherwise write standard SWE common data stream
            if (!dataWritten)
            {
                OutputStream os = new BufferedOutputStream(request.getResponseStream());
                
                // write small xml wrapper if requested
                if (((GetResultRequest) request).isXmlWrapper())
                {
                    String nsUri = OGCRegistry.getNamespaceURI(SOSUtils.SOS, request.getVersion());
                    os.write(new String("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n").getBytes());
                    os.write(new String("<GetResultResponse xmlns=\"" + nsUri + "\">\n<resultValues>\n").getBytes());
                }
                
                // set response headers in case of HTTP response
                else if (request.getHttpResponse() != null)
                {
                    if (resultEncoding instanceof TextEncoding)
                        request.getHttpResponse().setContentType(TEXT_MIME_TYPE);
                    else if (resultEncoding instanceof JSONEncoding)
                        request.getHttpResponse().setContentType(OWSUtils.JSON_MIME_TYPE);
                    else if (resultEncoding instanceof XMLEncoding)
                        request.getHttpResponse().setContentType(OWSUtils.XML_MIME_TYPE);
                    else if (resultEncoding instanceof BinaryEncoding)
                        request.getHttpResponse().setContentType(BINARY_MIME_TYPE);
                    else
                        throw new RuntimeException("Unsupported encoding: " + resultEncoding.getClass().getCanonicalName());
                }

                // prepare writer for selected encoding
                DataStreamWriter writer = SWEHelper.createDataWriter(resultEncoding);
                
                // we also do filtering here in case data provider hasn't modified the datablocks
                request.getObservables().add(SWEConstants.DEF_SAMPLING_TIME); // always keep sampling time
                writer = new FilteredWriter((AbstractDataWriter)writer, request.getObservables());
                writer.setDataComponents(resultStructure);
                writer.setOutput(os);
                
                // write each record in output stream
                DataBlock nextRecord;
                while ((nextRecord = dataProvider.getNextResultRecord()) != null)
                {
                    writer.write(nextRecord);
                    writer.flush();
                }
                
                // close xml wrapper
                if (((GetResultRequest) request).isXmlWrapper())
                    os.write(new String("\n</resultValues>\n</GetResultResponse>").getBytes());          
                        
                os.flush();
            }
        }
        finally
        {
            if (dataProvider != null)
                dataProvider.close();
        }
    }
    
    
    @Override
    protected void handleRequest(final GetFeatureOfInterestRequest request) throws Exception
    {
        OWSExceptionReport report = new OWSExceptionReport();
        Set<String> selectedProcedures = new LinkedHashSet<String>();
                
        // get list of procedures to scan
        List<String> procedures = request.getProcedures();
        if (procedures != null && !procedures.isEmpty())
        {
            // check listed procedures are valid
            for (String procID: procedures)
                checkQueryProcedure(procID, report);
                        
            selectedProcedures.addAll(procedures);
        }
        else
        {
            // otherwise just include all procedures
            selectedProcedures.addAll(procedureToOfferingMap.keySet());
        }
        
        // process observed properties
        List<String> observables = request.getObservables();
        if (observables != null && !observables.isEmpty())
        {
            // first check observables are valid in at least one offering
            for (String obsProp: observables)
            {
                boolean found = false;
                for (SOSOfferingCapabilities offering: capabilities.getLayers())
                {
                    if (offering.getObservableProperties().contains(obsProp))
                    {
                        found = true;
                        break;
                    }
                }
                
                if (!found)
                    report.add(new SOSException(SOSException.invalid_param_code, "observedProperty", obsProp, "Observed property " + obsProp + " is not available"));
            }
            
            // keep only procedures with selected observed properties            
            Iterator<String> it = selectedProcedures.iterator();
            while (it.hasNext())
            {
                String offeringID = procedureToOfferingMap.get(it.next());
                SOSOfferingCapabilities offering = offeringCaps.get(offeringID);
                
                boolean found = false;
                for (String obsProp: observables)
                {
                    offering.getObservableProperties().contains(obsProp);
                    found = true;
                    break;
                }
                
                if (!found)
                    it.remove();
            }
        }
        
        // if errors were detected, send them now
        report.process();
        
        // prepare feature filter
        final Polygon poly;
        if (request.getSpatialFilter() != null)
            poly = request.getBbox().toJtsPolygon();
        else
            poly = null;
        
        IFoiFilter filter = new FoiFilter()
        {
            public Polygon getRoi() { return poly; }
            public Collection<String> getFeatureIDs() { return request.getFoiIDs(); };
        };
        
        // init xml document writing
        OutputStream os = new BufferedOutputStream(request.getResponseStream());
        XMLOutputFactory factory = XMLImplFinder.getStaxOutputFactory();
        factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
        XMLStreamWriter xmlWriter = factory.createXMLStreamWriter(os, StandardCharsets.UTF_8.name());
        
        // prepare GML features writing
        GMLStaxBindings gmlBindings = new GMLStaxBindings();
        gmlBindings.registerFeatureBindings(new SMLStaxBindings());
        gmlBindings.declareNamespacesOnRootElement();        
        
        // start XML response
        xmlWriter.writeStartDocument();
        
        // wrap with SOAP envelope if requested
        startSoapEnvelope(request, xmlWriter);
        
        // write response root element
        String sosNsUri = OGCRegistry.getNamespaceURI(SOSUtils.SOS, DEFAULT_VERSION);
        xmlWriter.writeStartElement(SOS_PREFIX, "GetFeatureOfInterestResponse", sosNsUri);
        xmlWriter.writeNamespace(SOS_PREFIX, sosNsUri);
        gmlBindings.writeNamespaces(xmlWriter);        
        
        // scan offering corresponding to each selected procedure
        boolean first = true;
        HashSet<String> returnedFids = new HashSet<String>();
        
        for (String procID: selectedProcedures)
        {
            ISOSDataProviderFactory provider = getDataProviderFactoryBySensorID(procID);
            
            // output selected features
            Iterator<AbstractFeature> it2 = provider.getFoiIterator(filter);
            while (it2.hasNext())
            {
                AbstractFeature f = it2.next();
                
                // make sure we don't send twice the same feature
                if (returnedFids.contains(f.getUniqueIdentifier()))
                    continue;
                returnedFids.add(f.getUniqueIdentifier());
                
                // write namespace on root because in many cases it is common to all features
                if (first)
                {
                    gmlBindings.ensureNamespaceDecl(xmlWriter, f.getQName());
                    if (f instanceof GenericFeature)
                    {
                        for (Entry<QName, Object> prop: ((GenericFeature)f).getProperties().entrySet())
                            gmlBindings.ensureNamespaceDecl(xmlWriter, prop.getKey());
                    }
                    
                    first = false;
                }
                
                xmlWriter.writeStartElement(sosNsUri, "featureMember");
                gmlBindings.writeAbstractFeature(xmlWriter, f);
                xmlWriter.writeEndElement();
                xmlWriter.flush();
                os.write('\n');
            }
        }
        
        // close SOAP elements
        endSoapEnvelope(request, xmlWriter);
                
        xmlWriter.writeEndDocument();
        xmlWriter.close();
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
            
            // security check
            securityHandler.check(securityHandler.sos_insert_sensor);
           
            // choose offering name (here derived from sensor ID)
            String sensorUID = request.getProcedureDescription().getUniqueIdentifier();
            if (sensorUID == null)
                throw new SOSException(SOSException.missing_param_code, "identifier", null, "Missing unique identifier in SensorML description");
                        
            // add new offering, provider and virtual sensor if sensor is not already registered
            String offeringID = procedureToOfferingMap.get(sensorUID);
            if (offeringID == null)
            {
                ModuleRegistry moduleReg = SensorHub.getInstance().getModuleRegistry();
                ArrayList<ModuleConfig> configSaveList = new ArrayList<ModuleConfig>(3);
                configSaveList.add(this.config); 
                
                offeringID = sensorUID + "-sos";
                String sensorName = request.getProcedureDescription().getName();
                if (sensorName == null)
                    sensorName = request.getProcedureDescription().getId();
                
                // create and register new virtual sensor module if not already present
                if (!moduleReg.isModuleLoaded(sensorUID))
                {
                    SOSVirtualSensorConfig sensorConfig = new SOSVirtualSensorConfig();
                    sensorConfig.autoStart = false;
                    sensorConfig.id = sensorUID;
                    sensorConfig.name = sensorName;
                    SOSVirtualSensor virtualSensor = (SOSVirtualSensor)moduleReg.loadModule(sensorConfig);
                    sensorConfig.autoStart = true;
                    virtualSensor.requestInit(false);
                    virtualSensor.updateSensorDescription(request.getProcedureDescription(), false);                    
                    configSaveList.add(sensorConfig);
                }
                
                // start new virtual sensor module synchronously
                moduleReg.startModule(sensorUID, 1000);
                
                // generate new provider and consumer config
                SensorDataProviderConfig providerConfig = new SensorDataProviderConfig();
                providerConfig.enabled = true;
                providerConfig.sensorID = sensorUID;
                providerConfig.uri = offeringID;
                config.dataProviders.add(providerConfig);
                
                SensorConsumerConfig consumerConfig = new SensorConsumerConfig();
                consumerConfig.enabled = true;
                consumerConfig.offering = offeringID;
                consumerConfig.sensorID = sensorUID;
                config.dataConsumers.add(consumerConfig);
                
                // when new storage creation is enabled
                if (config.newStorageConfig != null)
                {
                    String storageID = sensorUID + "#storage";
                    
                    // create data storage if not already configured
                    if (!moduleReg.isModuleLoaded(storageID))
                    {
                        // create new storage module
                        StreamStorageConfig streamStorageConfig = new StreamStorageConfig();
                        streamStorageConfig.id = storageID;
                        streamStorageConfig.name = sensorName + " Storage";
                        streamStorageConfig.autoStart = true;
                        streamStorageConfig.dataSourceID = sensorUID;
                        streamStorageConfig.storageConfig = (StorageConfig)config.newStorageConfig.clone();
                        streamStorageConfig.storageConfig.storagePath = FileUtils.safeFileName(sensorUID) + ".dat";
                        moduleReg.loadModule(streamStorageConfig);
                        configSaveList.add(streamStorageConfig);
                        
                        /*// also add related features to storage
                        if (storage instanceof IObsStorage)
                        {
                            for (FeatureRef featureRef: request.getRelatedFeatures())
                                ((IObsStorage) storage).storeFoi(featureRef.getTarget());
                        }*/
                    }
                                        
                    // associate storage to config                    
                    providerConfig.storageID = storageID;
                    consumerConfig.storageID = storageID;
                    
                    // save config so that components stay active after restart
                    moduleReg.saveConfiguration(configSaveList.toArray(new ModuleConfig[0]));
                }
                
                // instantiate and register provider
                ISOSDataProviderFactory provider = providerConfig.getFactory(this);
                dataProviders.put(offeringID, provider);
                
                // instantiate and register consumer
                ISOSDataConsumer consumer = consumerConfig.getConsumerInstance();
                dataConsumers.put(offeringID, consumer);
                
                // create offering only if not already done when registering
                // the provider factory listener (if data source was in STARTED
                // state it can automatically trigger a call to showProviderCaps).
                if (!offeringCaps.containsKey(offeringID))
                    showProviderCaps(provider);
            }
            else
            {
                // get consumer and update
                ISOSDataConsumer consumer = getDataConsumerBySensorID(sensorUID);                
                consumer.updateSensor(request.getProcedureDescription());
            }
            
            // build and send response
            InsertSensorResponse resp = new InsertSensorResponse();
            resp.setAssignedOffering(offeringID);
            resp.setAssignedProcedureId(sensorUID);
            sendResponse(request, resp);
        }
        finally
        {
            
        }        
    }
    
    
    protected void startSoapEnvelope(OWSRequest request, XMLStreamWriter writer) throws XMLStreamException
    {
        String soapUri = request.getSoapVersion(); 
        if (soapUri != null)
        {
            writer.writeStartElement(SOAP_PREFIX, "Envelope", soapUri);
            writer.writeNamespace(SOAP_PREFIX, soapUri);
            writer.writeStartElement(SOAP_PREFIX, "Body", soapUri);
        }
    }
    
    
    protected void endSoapEnvelope(OWSRequest request, XMLStreamWriter writer) throws XMLStreamException
    {
        String soapUri = request.getSoapVersion(); 
        if (soapUri != null)
        {
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }
    
    
    protected boolean writeCustomFormatStream(GetResultRequest request, ISOSDataProvider dataProvider) throws Exception
    {
        String format = request.getFormat();
        
        // auto select video format in some common cases
        if (format == null)
        {
            DataEncoding resultEncoding = dataProvider.getDefaultResultEncoding();
            if (resultEncoding instanceof BinaryEncoding)
            {
                List<BinaryMember> mbrList = ((BinaryEncoding)resultEncoding).getMemberList();
                BinaryBlock videoFrameSpec = null;
                
                // try to find binary block encoding def in list
                for (BinaryMember spec: mbrList)
                {
                    if (spec instanceof BinaryBlock)
                    {
                        videoFrameSpec = (BinaryBlock)spec;
                        break;
                    }
                }
                        
                if (videoFrameSpec != null)
                {            
                    if (isRequestFromBrowser(request) && videoFrameSpec.getCompression().equals("H264"))
                        format = "video/mp4";
                    
                    else if (isRequestFromBrowser(request) && videoFrameSpec.getCompression().equals("JPEG"))
                        format = "video/x-motion-jpeg";            
                }
            }
        }
        
        // try to find matching implementation for selected format
        if (format != null)
        {
            ISOSCustomSerializer serializer = customFormats.get(format);
            
            // if not in map try to get a matching format implementation
            if (!customFormats.containsKey(format))
            {                
                for (SOSCustomFormatConfig allowedFormat: config.customFormats)
                {
                    if (allowedFormat.mimeType.equals(format))
                    {
                        ModuleRegistry moduleReg = SensorHub.getInstance().getModuleRegistry();
                        serializer = (ISOSCustomSerializer)moduleReg.loadClass(allowedFormat.className);
                    }
                }
                
                // save to map
                customFormats.put(format, serializer);
            }            
            
            if (serializer != null)
            {
                serializer.write(dataProvider, request);
                return true;
            }
            else
                throw new SOSException(SOSException.invalid_param_code, "format", format, "Unsupported format " + format);
        }
        
        return false;
    }
    
    
    /*
     * Check if request comes from a compatible browser
     */
    protected boolean isRequestFromBrowser(GetResultRequest request)
    {
        // don't do multipart with websockets
        HttpServletRequest httpRequest = request.getHttpRequest();
        if (httpRequest == null)
            return false;
        if (request.getHttpResponse() == null)
            return false;
        
        String userAgent = httpRequest.getHeader("User-Agent");
        if (userAgent == null)
            return false;
        
        if (userAgent.contains("Firefox"))
            return true;
        if (userAgent.contains("Chrome"))
            return true;
        
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
            ISOSDataConsumer consumer = getDataConsumerByOfferingID(offering);
            
            // security check
            securityHandler.check(offering, securityHandler.sos_insert_obs);
            
            // get template ID
            // the same template ID is always returned for a given observable            
            String templateID = consumer.newResultTemplate(request.getResultStructure(),
                                                           request.getResultEncoding(),
                                                           request.getObservationTemplate());
                        
            // only continue of template was not already registered
            if (!templateToOfferingMap.containsKey(templateID))
            {
                templateToOfferingMap.put(templateID, offering);
                
                // re-generate capabilities
                ISOSDataProviderFactory provider = getDataProviderFactoryByOfferingID(offering);
                SOSOfferingCapabilities newCaps = provider.generateCapabilities();
                int oldIndex = 0;
                for (OWSLayerCapabilities offCaps: capabilities.getLayers())
                {
                    if (offCaps.getIdentifier().equals(offering))
                        break; 
                    oldIndex++;
                }
                capabilities.getLayers().set(oldIndex, newCaps);
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
                encoding = SWEHelper.ensureXmlCompatible(encoding);
                resultStream = dataSrc.getDataStream();
            }
            else // POST body
            {
                resultStream = new BufferedInputStream(request.getHttpRequest().getInputStream());
            }
            
            // create parser
            parser = SWEHelper.createDataParser(encoding);
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


    protected void checkQueryOfferings(List<String> offerings, OWSExceptionReport report) throws SOSException
    {
        try
        {
            capabilitiesLock.readLock().lock();
            
            for (String offering: offerings)
            {
                if (!offeringCaps.containsKey(offering))
                    report.add(new SOSException(SOSException.invalid_param_code, "offering", offering, "Offering " + offering + " is not available on this server"));
            }
        }
        finally
        {
            capabilitiesLock.readLock().unlock();
        }   
    }
    
    
    protected void checkQueryObservables(String offeringID, List<String> observables, OWSExceptionReport report) throws SOSException
    {
        SWESOfferingCapabilities offering = checkAndGetOffering(offeringID);
        for (String obsProp: observables)
        {
            if (!offering.getObservableProperties().contains(obsProp))
                report.add(new SOSException(SOSException.invalid_param_code, "observedProperty", obsProp, "Observed property " + obsProp + " is not available for offering " + offeringID));
        }
    }
    
    
    protected void checkQueryObservables(List<String> observables, OWSExceptionReport report) throws SOSException
    {
        for (String obsProp: observables)
        {
            boolean found = false;
            
            for (SOSOfferingCapabilities offering: offeringCaps.values())
            {            
                if (offering.getObservableProperties().contains(obsProp))
                {
                    found = true;
                    break;
                }
            }
            
            if (!found)
                report.add(new SOSException(SOSException.invalid_param_code, "observedProperty", obsProp, "Observed property " + obsProp + " is not available on this server"));
        }   
    }


    protected void checkQueryProcedures(String offeringID, List<String> procedures, OWSExceptionReport report) throws SOSException
    {
        SWESOfferingCapabilities offering = checkAndGetOffering(offeringID);
        for (String procID: procedures)
        {
            if (!offering.getProcedures().contains(procID))
                report.add(new SOSException(SOSException.invalid_param_code, "procedure", procID, "Procedure " + procID + " is not available for offering " + offeringID));
        }
    }
    
    
    protected void checkQueryProcedures(List<String> procedures, OWSExceptionReport report) throws SOSException
    {
        for (String procID: procedures)
        {
            boolean found = false;
            
            for (SOSOfferingCapabilities offering: offeringCaps.values())
            {            
                if (offering.getProcedures().contains(procID))
                {
                    found = true;
                    break;
                }
            }
            
            if (!found)
                report.add(new SOSException(SOSException.invalid_param_code, "procedure", procID, "Procedure " + procID + " is not available on this server"));
        }   
    }


    protected void checkQueryFormat(String offeringID, String format, OWSExceptionReport report) throws SOSException
    {
        SOSOfferingCapabilities offering = checkAndGetOffering(offeringID);
        if (!offering.getResponseFormats().contains(format))
            report.add(new SOSException(SOSException.invalid_param_code, "format", format, "Format " + format + " is not available for offering " + offeringID));
    }


    protected void checkQueryTime(String offeringID, TimeExtent requestTime, OWSExceptionReport report) throws SOSException
    {
        SOSOfferingCapabilities offering = checkAndGetOffering(offeringID);
        
        if (requestTime.isNull())
            return;
        
        // make sure startTime <= stopTime
        if (requestTime.getStartTime() > requestTime.getStopTime())
            report.add(new SOSException("The requested period must begin before the it ends"));
            
        // refresh offering capabilities if needed
        try
        {
            ISOSDataProviderFactory provider = (ISOSDataProviderFactory)dataProviders.get(offeringID);
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
    
    
    protected SOSOfferingCapabilities checkAndGetOffering(String offeringID) throws SOSException
    {
        try
        {
            capabilitiesLock.readLock().lock();
            SOSOfferingCapabilities offCaps = offeringCaps.get(offeringID);
            
            if (offCaps == null)
                throw new SOSException(SOSException.invalid_param_code, "offering", offeringID, null);
            
            return offCaps;
        }
        finally
        {
            capabilitiesLock.readLock().unlock();
        }
    }
    
    
    protected ISOSDataProvider getDataProvider(String offering, SOSDataFilter filter) throws Exception
    {
        try
        {
            capabilitiesLock.readLock().lock();
            
            checkAndGetOffering(offering);
            ISOSDataProviderFactory factory = dataProviders.get(offering);
            if (factory == null)
                throw new IllegalStateException("No valid data provider factory found for offering " + offering);
            return factory.getNewDataProvider(filter);
        }
        finally
        {
            capabilitiesLock.readLock().unlock();
        }
    }
    
    
    protected ISOSDataProviderFactory getDataProviderFactoryByOfferingID(String offering) throws Exception
    {
        try
        {
            capabilitiesLock.readLock().lock();
            
            ISOSDataProviderFactory factory = dataProviders.get(offering);
            if (factory == null)
                throw new IllegalStateException("No valid data provider factory found for offering " + offering);
            return (ISOSDataProviderFactory)factory;
        }
        finally
        {
            capabilitiesLock.readLock().unlock();
        }
    }
    
    
    protected ISOSDataProviderFactory getDataProviderFactoryBySensorID(String sensorID) throws Exception
    {
        try
        {
            capabilitiesLock.readLock().lock();            
            String offering = procedureToOfferingMap.get(sensorID);
            return getDataProviderFactoryByOfferingID(offering);
        }
        finally
        {
            capabilitiesLock.readLock().unlock();
        }
    }
    
    
    protected ISOSDataConsumer getDataConsumerByOfferingID(String offering) throws Exception
    {
        try
        {
            capabilitiesLock.readLock().lock();
            
            checkAndGetOffering(offering);
            ISOSDataConsumer consumer = dataConsumers.get(offering);
            
            if (consumer == null)
                throw new SOSException(SOSException.invalid_param_code, "offering", offering, "Transactional operations are not supported for offering " + offering);
                
            return consumer;
        }
        finally
        {
            capabilitiesLock.readLock().unlock();
        }
    }
    
    
    protected ISOSDataConsumer getDataConsumerBySensorID(String sensorID) throws Exception
    {
        try
        {
            capabilitiesLock.readLock().lock();
            
            String offering = procedureToOfferingMap.get(sensorID);
            if (offering == null)
                throw new SOSException(SOSException.invalid_param_code, "procedure", sensorID, "Transactional operations are not supported for sensor " + sensorID);
            
            return getDataConsumerByOfferingID(offering);
        }
        finally
        {
            capabilitiesLock.readLock().unlock();
        }
    }
    
    
    protected ISOSDataConsumer getDataConsumerByTemplateID(String templateID) throws Exception
    {
        try
        {
            capabilitiesLock.readLock().lock();
            
            String offering = templateToOfferingMap.get(templateID);
            ISOSDataConsumer consumer = dataConsumers.get(offering);
            if (consumer == null)
                throw new SOSException(SOSException.invalid_param_code, "template", templateID, "Invalid template ID");
            
            return consumer;
        }
        finally
        {
            capabilitiesLock.readLock().unlock();
        }
    }
    
    
    protected void checkTransactionalSupport(OWSRequest request) throws Exception
    {
        if (!config.enableTransactional)
            throw new SOSException(SOSException.invalid_param_code, "request", request.getOperation(), request.getOperation() + " operation is not supported on this endpoint"); 
    }


    @Override
    protected String getDefaultVersion()
    {
        return DEFAULT_VERSION;
    }
}
