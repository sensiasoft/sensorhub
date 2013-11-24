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
import org.sensorhub.api.service.IServiceInterface;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.HttpServer;
import org.sensorhub.impl.service.ogc.OGCServiceConfig.CapabilitiesInfo;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSExceptionReport;
import org.vast.ows.OWSRequest;
import org.vast.ows.OWSUtils;
import org.vast.ows.sos.GetObservationRequest;
import org.vast.ows.sos.GetResultRequest;
import org.vast.ows.sos.GetResultTemplateRequest;
import org.vast.ows.sos.SOSException;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.ows.sos.SOSServiceCapabilities;
import org.vast.ows.sos.SOSServlet;
import org.vast.ows.swe.DescribeSensorRequest;
import org.vast.sensorML.SMLUtils;
import org.vast.sensorML.system.SMLSystem;
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
public class SOSService extends SOSServlet implements IServiceInterface<SOSServiceConfig>, IEventListener
{
    private static final Log log = LogFactory.getLog(SOSService.class);    
    
    SOSServiceConfig config;
    SOSServiceCapabilities capabilitiesCache;
    Map<String, IDataProviderFactory> procedureToProviderMap;
    Map<String, SOSOfferingCapabilities> offeringMap;
    

    @Override
    public boolean isEnabled()
    {
        return config.enabled;
    }
    
    
    @Override
    public void init(SOSServiceConfig config) throws SensorHubException
    {        
        this.config = config;
        this.procedureToProviderMap = new HashMap<String, IDataProviderFactory>();
        this.offeringMap = new HashMap<String, SOSOfferingCapabilities>();
        
        // pre-generate capabilities
        this.capabilitiesCache = generateCapabilities();
        
        // deploy ourself to HTTP server
        HttpServer.getInstance().deployServlet(config.endPoint, this);
        
        // subscribe to server lifecycle events
        ModuleRegistry.getInstance().registerListener(this);
    }
    
    
    /**
     * Generates the SOSCapabilities object with info from data source
     * @return
     * @throws ServiceException
     */
    protected SOSServiceCapabilities generateCapabilities() throws ServiceException
    {
        procedureToProviderMap.clear();
        
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
        capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_RESULT_INSERTION);
        capabilities.getProfiles().add(SOSServiceCapabilities.PROFILE_OBS_INSERTION);        
        
        // process each provider config
        for (SOSProviderConfig providerConf: config.dataProviders)
        {
            if (!providerConf.enabled)
                continue;
            
            try
            {
                // instantiate provider factories and map them to offering URIs
                IDataProviderFactory factory = providerConf.getFactory();
                dataProviders.put(providerConf.uri, factory);
     
                // add offering metadata to capabilities
                SOSOfferingCapabilities offCaps = factory.generateCapabilities();
                capabilities.getLayers().add(offCaps);
                offeringMap.put(offCaps.getIdentifier(), offCaps);
                
                // build procedure map
                procedureToProviderMap.put(offCaps.getProcedures().get(0), factory);
                
                if (log.isDebugEnabled())
                    log.debug("Offering generated for procedure " + offCaps.getProcedures().get(0) + ":\n" + offCaps.toString());
            }
            catch (Exception e)
            {
                log.error("Error while generating capabilities for provider " + providerConf.uri, e);
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
            IDataProviderFactory factory = procedureToProviderMap.get(uri);
            return (SMLSystem)factory.generateSensorMLDescription(null);
        }
        catch (Exception e)
        {
            throw new ServiceException("Error while retrieving SensorML description for sensor " + uri, e);
        }
    }
    
    
    @Override
    public synchronized void updateConfig(SOSServiceConfig config) throws SensorHubException
    {
        // cleanup all previously instantiated providers
        
        
        // rebuild everything

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
                config.enabled = true;
        }  
        
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {
        // clean all providers
        for (IDataProviderFactory provider: procedureToProviderMap.values())
            provider.cleanup();
        
        // undeploy ourself
        
    }
    
    /////////////////////////////////////////
    /// methods overriden from SOSServlet ///
    /////////////////////////////////////////
    
    @Override
    protected void handleRequest(GetCapabilitiesRequest request) throws Exception
    {
        DOMHelper dom = new DOMHelper();
        OWSUtils utils = new OWSUtils();
        Element respElt = utils.buildXMLResponse(dom, generateCapabilities(), request.getVersion());
        dom.serialize(respElt, request.getResponseStream(), true);
    }
        
    
    @Override
    protected synchronized void handleRequest(DescribeSensorRequest request) throws Exception
    {
        String sensorID = request.getProcedureID();
        if (sensorID == null || !procedureToProviderMap.containsKey(sensorID))
            throw new SOSException(SOSException.invalid_param_code, "procedure", sensorID, null);
        
        DOMHelper dom = new DOMHelper();
        SMLUtils smlUtils = new SMLUtils();
        Element respElt = smlUtils.writeSystem(dom, generateSensorML(sensorID));
        dom.serialize(respElt, request.getResponseStream(), null);
    }


    // overriden to add synchronization
    @Override
    protected synchronized void handleRequest(GetResultTemplateRequest request) throws Exception
    {
        super.handleRequest(request);
    }


    // overriden to add synchronization
    @Override
    protected synchronized void handleRequest(GetResultRequest request) throws Exception
    {
        super.handleRequest(request);
    }


    // overriden to add synchronization
    @Override
    protected synchronized void handleRequest(GetObservationRequest request) throws Exception
    {
        super.handleRequest(request);
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
    
    
    protected SOSOfferingCapabilities checkAndGetOffering(String offeringID) throws SOSException
    {
        SOSOfferingCapabilities offCaps = offeringMap.get(offeringID);
        
        if (offCaps == null)
            throw new SOSException(SOSException.invalid_param_code, "offering", offeringID, null);
        
        return offCaps;
    }

}
