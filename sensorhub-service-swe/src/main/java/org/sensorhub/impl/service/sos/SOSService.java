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

import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.service.IServiceInterface;
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.ows.sos.ISOSDataProviderFactory;
import org.vast.ows.sos.SOSServlet;


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
public class SOSService extends SOSServlet implements IServiceInterface<SOSServiceConfig>, ISOSDataProviderFactory
{
    SOSServiceConfig config;
    Map<String, SOSProviderConfig> dataProviderConfigs;
    

    @Override
    public void init(SOSServiceConfig config) throws SensorHubException
    {        
        dataProviderConfigs = new LinkedHashMap<String, SOSProviderConfig>();
        
        for (SOSProviderConfig providerConf: config.dataProviders)
        {
            // TODO deal with null URI ??            
            dataProviderConfigs.put(providerConf.uri, providerConf);
            
            // register to source events
            if (providerConf instanceof SensorDataProviderConfig)
            {
                //((SensorDataProviderConfig)providerConf).sensorID;
                
            }
        }
    }


    @Override
    public void updateConfig(SOSServiceConfig config) throws SensorHubException
    {
        // TODO Auto-generated method stub

    }
    
    
    @Override
    public ISOSDataProvider getNewProvider(SOSDataFilter filter) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    
    @Override
    public SOSServiceConfig getConfiguration()
    {
        return (SOSServiceConfig)config.clone();
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
    public void cleanup() throws SensorHubException
    {
        // TODO Auto-generated method stub
        
    }
    
    /////////////////////////////////////////
    /// methods overriden from SOSServlet ///
    /////////////////////////////////////////
    
    @Override
    protected void sendCapabilities(String section, OutputStream resp)
    {
        // TODO generate capabilities from config + sensor info
    }

}
