/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sps;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.impl.service.ogc.OGCServiceConfig.CapabilitiesInfo;
import org.slf4j.Logger;
import org.vast.data.DataBlockList;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSExceptionReport;
import org.vast.ows.OWSRequest;
import org.vast.ows.server.OWSServlet;
import org.vast.ows.sos.SOSException;
import org.vast.ows.sps.*;
import org.vast.ows.sps.StatusReport.TaskStatus;
import org.vast.ows.swe.DescribeSensorRequest;
import org.vast.ows.util.PostRequestFilter;
import org.vast.sensorML.SMLUtils;
import org.vast.xml.DOMHelper;
import org.w3c.dom.Element;


/**
 * <p>
 * Implementation of SensorHub generic SPS service.
 * The service can manage any of the sensors installed on the SensorHub instance
 * and is configured automatically from the information generated by the sensors
 * interfaces.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jan 15, 2015
 */
@SuppressWarnings("serial")
public class SPSServlet extends OWSServlet
{
    protected static final String DEFAULT_VERSION = "2.0.0";
    
    SPSServiceConfig config;
    SPSSecurity securityHandler;
    Logger log;
    String endpointUrl;
    ReentrantReadWriteLock capabilitiesLock = new ReentrantReadWriteLock();
    SPSServiceCapabilities capabilities;
    Map<String, ISPSConnector> connectors;
    Map<String, ISPSConnector> procedureToConnectorMap;
    Map<String, SPSOfferingCapabilities> procedureToOfferingMap;
    IEventHandler eventHandler;
    
    SMLUtils smlUtils = new SMLUtils(SMLUtils.V2_0);
    ITaskDB taskDB;
    //SPSNotificationSystem notifSystem;
    
    ModuleState state;
    
    
    public SPSServlet(SPSServiceConfig config, SPSSecurity securityHandler, Logger log)
    {
        this.config = config;
        this.securityHandler = securityHandler;
        this.log = log;
        this.owsUtils = new SPSUtils();
    }
    
    
    protected void start() throws SensorHubException
    {
        this.connectors = new LinkedHashMap<String, ISPSConnector>();
        this.procedureToConnectorMap = new HashMap<String, ISPSConnector>();
        this.procedureToOfferingMap = new HashMap<String, SPSOfferingCapabilities>();
        this.taskDB = new InMemoryTaskDB();
        
        // pre-generate capabilities
        endpointUrl = null;
        generateCapabilities();       
    }
    
    
    protected void stop()
    {
        // clean all connectors
        for (ISPSConnector connector: connectors.values())
            connector.cleanup();       
    }
    
    
    /**
     * Generates the SPSServiceCapabilities object with info obtained from connector
     */
    protected void generateCapabilities()
    {
        connectors.clear();
        procedureToConnectorMap.clear();
        procedureToOfferingMap.clear();
        capabilities = new SPSServiceCapabilities();
        
        // get main capabilities info from config
        CapabilitiesInfo serviceInfo = config.ogcCapabilitiesInfo;
        capabilities.getIdentification().setTitle(serviceInfo.title);
        capabilities.getIdentification().setDescription(serviceInfo.description);
        capabilities.setFees(serviceInfo.fees);
        capabilities.setAccessConstraints(serviceInfo.accessConstraints);
        capabilities.setServiceProvider(serviceInfo.serviceProvider);
        
        // supported operations
        capabilities.getGetServers().put("GetCapabilities", config.endPoint);
        capabilities.getGetServers().put("DescribeSensor", config.endPoint);
        capabilities.getPostServers().putAll(capabilities.getGetServers());
        capabilities.getPostServers().put("Submit", config.endPoint);
        
        // generate profile list
        /*capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_RESULT_RETRIEVAL);
        if (config.enableTransactional)
        {
            capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_RESULT_INSERTION);
            capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_OBS_INSERTION);
        }*/
        
        // process each provider config
        if (config.connectors != null)
        {
            for (SPSConnectorConfig connectorConf: config.connectors)
            {
                try
                {
                    // instantiate provider factories and map them to offering URIs
                    ISPSConnector connector = connectorConf.getConnector(this);
                    connectors.put(connectorConf.uri, connector);
                    
                    // create offering only if not already done when registering
                    // the connector listener (if command receiver was in STARTED
                    // state it can automatically trigger a call to showConnectorCaps).
                    if (!procedureToConnectorMap.containsValue(connector) && connector.isEnabled())
                        showConnectorCaps(connector);
                }
                catch (Exception e)
                {
                    log.error("Error while initializing connector " + connectorConf.uri, e);
                }
            }
        }
    }
    
    
    protected void showConnectorCaps(ISPSConnector connector)
    {
        SPSConnectorConfig config = connector.getConfig();
        
        try
        {
            capabilitiesLock.writeLock().lock();
            
            // generate offering metadata
            SPSOfferingCapabilities offCaps = connector.generateCapabilities();
            String procedureID = offCaps.getMainProcedure();
            
            // update offering if it was already advertised
            if (procedureToOfferingMap.containsKey(procedureID))
            {
                // replace old offering
                SPSOfferingCapabilities oldCaps = procedureToOfferingMap.put(procedureID, offCaps);
                capabilities.getLayers().set(capabilities.getLayers().indexOf(oldCaps), offCaps);
                
                if (log.isDebugEnabled())
                    log.debug("Offering " + "\"" + offCaps.getIdentifier() + "\" updated for procedure " + procedureID);
            }
            
            // otherwise add new offering
            else
            {
                // add to maps and layer list
                procedureToOfferingMap.put(procedureID, offCaps);
                procedureToConnectorMap.put(procedureID, connector);
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
    
    
    protected void hideConnectorCaps(ISPSConnector connector)
    {
        try
        {
            capabilitiesLock.writeLock().lock();
            
            // get procedure ID
            String procID = null;
            for (Entry<String, ISPSConnector> entry: procedureToConnectorMap.entrySet())
            {
                if (entry.getValue() == connector)
                {
                    procID = entry.getKey();
                    break;
                }
            }
            
            // stop here if connector is not advertised
            if (procID == null)
                return;
            
            // remove offering from capabilities
            SPSOfferingCapabilities offCaps = procedureToOfferingMap.remove(procID);
            capabilities.getLayers().remove(offCaps);
            
            // remove connector
            procedureToConnectorMap.remove(procID);
            
            if (log.isDebugEnabled())
                log.debug("Offering " + "\"" + offCaps.getIdentifier() + "\" removed for procedure " + procID);
        }
        finally
        {
            capabilitiesLock.writeLock().unlock();
        }
    }
    
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // set current authentified user
        if (req.getRemoteUser() != null)
            securityHandler.setCurrentUser(req.getRemoteUser());
        else
            securityHandler.setCurrentUser(ISecurityManager.ANONYMOUS_USER);
        
        try
        {
            super.service(req, resp);
        }
        finally
        {
            securityHandler.clearCurrentUser();
        }
    }
    
    
    @Override
    protected OWSRequest parseRequest(HttpServletRequest req, HttpServletResponse resp, boolean post) throws Exception
    {
        if (post)
        {
            InputStream xmlRequest = new PostRequestFilter(new BufferedInputStream(req.getInputStream()));
            DOMHelper dom = new DOMHelper(xmlRequest, false);
            
            Element requestElt = dom.getBaseElement();
            OWSRequest owsRequest;
            
            // detect and skip SOAP envelope if present
            String soapVersion = getSoapVersion(dom);
            if (soapVersion != null)
                requestElt = getSoapBody(dom);
            
            // case of tasking request, need to get tasking params for the selected procedure
            if (isTaskingRequest(requestElt))
            {
                String procID = dom.getElementValue(requestElt, "procedure");
                SPSOfferingCapabilities offering = procedureToOfferingMap.get(procID);
                if (offering == null)
                    throw new SPSException(SPSException.invalid_param_code, "procedure", procID);
                DescribeTaskingResponse paramDesc = offering.getParametersDescription();
                
                // use full tasking params or updatable subset
                DataComponent taskingParams;
                if (requestElt.getLocalName().equals("Update"))
                    taskingParams = paramDesc.getUpdatableParameters();
                else
                    taskingParams = paramDesc.getTaskingParameters();
                
                owsRequest = ((SPSUtils)owsUtils).readSpsRequest(dom, requestElt, taskingParams);
            }
            
            // case of normal request
            else
                owsRequest = owsUtils.readXMLQuery(dom, requestElt);
            
            if (soapVersion != null)
                owsRequest.setSoapVersion(soapVersion);
            
            return owsRequest;
        }
        else
        {
            return super.parseRequest(req, resp, post);
        }
    }
    
    
    protected boolean isTaskingRequest(Element requestElt)
    {
        String localName = requestElt.getLocalName();
        
        if (localName.equals("GetFeasibility"))
            return true;
        else if (localName.equals("Submit"))
            return true;
        else if (localName.equals("Update"))
            return true;
        else if (localName.equals("Reserve"))
            return true;
        
        return false;
    }
    
    
    @Override
    protected void handleRequest(OWSRequest request) throws Exception
    {
        if (request instanceof GetCapabilitiesRequest)
            handleRequest((GetCapabilitiesRequest)request);
        else if (request instanceof DescribeSensorRequest)
            handleRequest((DescribeSensorRequest)request);
        else if (request instanceof DescribeTaskingRequest)
            handleRequest((DescribeTaskingRequest)request);
        else if (request instanceof GetStatusRequest)
            handleRequest((GetStatusRequest)request);
        else if (request instanceof GetFeasibilityRequest)
            handleRequest((GetFeasibilityRequest)request);
        else if (request instanceof SubmitRequest)
            handleRequest((SubmitRequest)request);
        else if (request instanceof UpdateRequest)
            handleRequest((UpdateRequest)request);
        else if (request instanceof CancelRequest)
            handleRequest((CancelRequest)request);
        else if (request instanceof ReserveRequest)
            handleRequest((ReserveRequest)request);
        else if (request instanceof ConfirmRequest)
            handleRequest((ConfirmRequest)request);
        else if (request instanceof DescribeResultAccessRequest)
            handleRequest((DescribeResultAccessRequest)request);
    }
    
    
    protected void handleRequest(GetCapabilitiesRequest request) throws Exception
    {
        /*// check that version 2.0.0 is supported by client
        if (!request.getAcceptedVersions().isEmpty())
        {
            if (!request.getAcceptedVersions().contains(DEFAULT_VERSION))
                throw new SOSException(SOSException.version_nego_failed_code, "AcceptVersions", null,
                        "Only version " + DEFAULT_VERSION + " is supported by this server");
        }
        
        // set selected version
        request.setVersion(DEFAULT_VERSION);*/
        
        // security check
        securityHandler.checkPermission(securityHandler.sps_read_caps);
        
        // update operation URLs
        if (endpointUrl == null)
        {
            try
            {
                capabilitiesLock.writeLock().lock();
            
                endpointUrl = request.getHttpRequest().getRequestURL().toString();
                for (Entry<String, String> op: capabilities.getGetServers().entrySet())
                    capabilities.getGetServers().put(op.getKey(), endpointUrl);
                for (Entry<String, String> op: capabilities.getPostServers().entrySet())
                    capabilities.getPostServers().put(op.getKey(), endpointUrl);
            }
            finally
            {            
                capabilitiesLock.writeLock().unlock();
            }
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
    
    
    protected void handleRequest(DescribeSensorRequest request) throws Exception
    {
        String procID = request.getProcedureID();
        
        OWSExceptionReport report = new OWSExceptionReport();
        ISPSConnector connector = getConnectorByProcedureID(procID, report);
        checkQueryProcedureFormat(procID, request.getFormat(), report);
        report.process();
        
        // security check
        securityHandler.checkPermission(getOfferingID(procID), securityHandler.sps_read_sensor);
        
        // serialize and send SensorML description
        OutputStream os = new BufferedOutputStream(request.getResponseStream());
        smlUtils.writeProcess(os, connector.generateSensorMLDescription(Double.NaN), true);
    }
    
    
    protected void handleRequest(DescribeTaskingRequest request) throws Exception
    {
        String procID = request.getProcedureID();
        SPSOfferingCapabilities offering = procedureToOfferingMap.get(procID);
        
        if (offering == null)
            throw new SPSException(SPSException.invalid_param_code, "procedure", procID);
        
        // security check
        securityHandler.checkPermission(offering.getIdentifier(), securityHandler.sps_read_params);
        
        sendResponse(request, offering.getParametersDescription());
    }
    
    
    protected ITask findTask(String taskID) throws SPSException
    {
        ITask task = taskDB.getTask(taskID);
        
        if (task == null)
            throw new SPSException(SPSException.invalid_param_code, "task", taskID);
        
        return task;
    }
    
    
    protected void handleRequest(GetStatusRequest request) throws Exception
    {
        ITask task = findTask(request.getTaskID());
        StatusReport status = task.getStatusReport();
        
        // security check
        String procID = task.getRequest().getProcedureID();
        securityHandler.checkPermission(getOfferingID(procID), securityHandler.sps_read_task);
        
        GetStatusResponse gsResponse = new GetStatusResponse();
        gsResponse.setVersion("2.0.0");
        gsResponse.getReportList().add(status);
        
        sendResponse(request, gsResponse);
    }
    
    
    protected GetFeasibilityResponse handleRequest(GetFeasibilityRequest request) throws Exception
    {               
        /*GetFeasibilityResponse gfResponse = new GetFeasibilityResponse();
        
        // create task in DB
        ITask newTask = taskDB.createNewTask(request);
        String studyId = newTask.getID();
        
        // launch feasibility study
        //FeasibilityResult result = doFeasibilityStudy(request);
        String sensorId = request.getSensorID();
        
        // create response
        GetFeasibilityResponse gfResponse = new GetFeasibilityResponse();
        gfResponse.setVersion("2.0.0");
        FeasibilityReport report = gfResponse.getReport();
        report.setTitle("Automatic Feasibility Results");
        report.setTaskID(studyId);
        report.setSensorID(sensorId);
                
        if (!isFeasible(result))
        {
            report.setRequestStatus(RequestStatus.Rejected);
        }
        else
        {
            report.setRequestStatus(RequestStatus.Accepted);
            report.setPercentCompletion(1.0f);            
        }
        
        report.touch();
        taskDB.updateTaskStatus(report);
        
        return gfResponse;*/  
        throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    
    
    protected void handleRequest(SubmitRequest request) throws Exception
    {
        // validate task parameters
        request.validate();
        
        // retrieve connector instance
        OWSExceptionReport report = new OWSExceptionReport();
        String procID = request.getProcedureID();
        ISPSConnector conn = getConnectorByProcedureID(procID, report);
        report.process();
        
        // security check
        securityHandler.checkPermission(getOfferingID(procID), securityHandler.sps_task_submit);
        
        // create task in DB
        ITask newTask = taskDB.createNewTask(request);
        final String taskID = newTask.getID();
        
        // send command through connector
        DataBlockList dataBlockList = (DataBlockList)request.getParameters().getData();
        Iterator<DataBlock> it = dataBlockList.blockIterator();
        while (it.hasNext())
            conn.sendSubmitData(newTask, it.next());        
        
        // add report and send response
        SubmitResponse sResponse = new SubmitResponse();
        sResponse.setVersion("2.0");
        ITask task = findTask(taskID);
        task.getStatusReport().setTaskStatus(TaskStatus.Completed);
        task.getStatusReport().touch();
        sResponse.setReport(task.getStatusReport());
        
        sendResponse(request, sResponse);
    }
    

    protected void handleRequest(UpdateRequest request) throws Exception
    {
        throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    
    
    protected void handleRequest(CancelRequest request) throws Exception
    {
        throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    

    protected void handleRequest(ReserveRequest request) throws Exception
    {
        throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    
    
    protected void handleRequest(ConfirmRequest request) throws Exception
    {
        throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    
    
    protected void handleRequest(DescribeResultAccessRequest request) throws Exception
    {
        /*ITask task = findTask(request.getTaskID());
        
        DescribeResultAccessResponse resp = new DescribeResultAccessResponse();     
        StatusReport status = task.getStatusReport();
        
        // TODO DescribeResultAccess
        
        return resp;*/
        throw new SPSException(SPSException.unsupported_op_code, request.getOperation());
    }
    
    
    protected final ISPSConnector getConnectorByProcedureID(String procedureID, OWSExceptionReport report) throws Exception
    {
        try
        {
            capabilitiesLock.readLock().lock();
            ISPSConnector connector = procedureToConnectorMap.get(procedureID);
            
            if (connector == null)
                report.add(new SPSException(SPSException.invalid_param_code, "procedure", procedureID));
            
            return connector;
        }
        finally
        {            
            capabilitiesLock.readLock().unlock();
        }
    }
    
    
    protected final String getOfferingID(String procedureID)
    {
        return procedureToOfferingMap.get(procedureID).getIdentifier();
    }
    
    
    protected void checkQueryProcedureFormat(String procedureID, String format, OWSExceptionReport report) throws SOSException
    {
        // ok if default format can be used
        if (format == null)
            return;
        
        SPSOfferingCapabilities offering = this.procedureToOfferingMap.get(procedureID);
        if (!offering.getProcedureFormats().contains(format))
            report.add(new SOSException(SOSException.invalid_param_code, "procedureDescriptionFormat", format, "Procedure description format " + format + " is not available for procedure " + procedureID));
    }


    @Override
    protected String getServiceType()
    {
        return SPSUtils.SPS;
    }


    @Override
    protected String getDefaultVersion()
    {
        return "2.0";
    }
}
