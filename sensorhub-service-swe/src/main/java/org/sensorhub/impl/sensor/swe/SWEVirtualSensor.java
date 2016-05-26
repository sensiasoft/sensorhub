/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.swe;

import java.util.ArrayList;
import java.util.List;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractPhysicalProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.client.sos.SOSClient;
import org.sensorhub.impl.client.sos.SOSClient.SOSRecordListener;
import org.sensorhub.impl.client.sps.SPSClient;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ows.GetCapabilitiesRequest;
import org.vast.ows.OWSException;
import org.vast.ows.OWSUtils;
import org.vast.ows.sos.GetResultRequest;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.ows.sos.SOSServiceCapabilities;
import org.vast.ows.sos.SOSUtils;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Driver for SWE enabled sensors communicating via SOS & SPS standard services.
 * This can also be used to communicate with a sensor deployed on another
 * (usually remote) sensor hub node.
 * TODO forward full SensorML description
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2015
 */
public class SWEVirtualSensor extends AbstractSensorModule<SWEVirtualSensorConfig>
{
    protected static final Logger log = LoggerFactory.getLogger(SWEVirtualSensor.class);
    private static final String SOS_VERSION = "2.0";
    private static final String SPS_VERSION = "2.0";
    private static final double STREAM_END_TIME = 2e9; // 
    
    AbstractFeature currentFoi;    
    List<SOSClient> sosClients;
    SPSClient spsClient;
        
    
    public SWEVirtualSensor()
    {
    }
    
    
    protected void checkConfig() throws SensorHubException
    {
        if (config.sensorUID == null)
            throw new SensorHubException("Sensor UID must be specified");
        
        if (config.sosEndpointUrl != null && config.observedProperties.size() == 0)
            throw new SensorHubException("At least one observed property must be specified");
    }
    
    
    @Override
    public void start() throws SensorHubException
    {        
        checkConfig();
        
        removeAllOutputs();
        removeAllControlInputs();        
        OWSUtils owsUtils = new OWSUtils();
        
        // create and start SOS clients
        if (config.sosEndpointUrl != null)
        {
            // find matching offering(s) for sensor UID
            SOSServiceCapabilities caps = null;
            try
            {
                GetCapabilitiesRequest getCap = new GetCapabilitiesRequest();
                getCap.setService(SOSUtils.SOS);
                getCap.setVersion(SOS_VERSION);
                getCap.setGetServer(config.sosEndpointUrl);
                caps = owsUtils.<SOSServiceCapabilities>sendRequest(getCap, false);
            }
            catch (OWSException e)
            {
                throw new SensorHubException("Cannot retrieve SOS capabilities", e);
            }
            
            // scan all offerings and connect to selected ones
            int outputNum = 1;
            sosClients = new ArrayList<SOSClient>(config.observedProperties.size());            
            for (SOSOfferingCapabilities offering: caps.getLayers())
            {
                if (offering.getMainProcedure().equals(config.sensorUID))
                {
                    String offeringID = offering.getIdentifier();
                    
                    for (String obsProp: config.observedProperties)
                    {
                        if (offering.getObservableProperties().contains(obsProp))
                        {                            
                            // create data request
                            GetResultRequest req = new GetResultRequest();
                            req.setGetServer(config.sosEndpointUrl);
                            req.setVersion(SOS_VERSION);
                            req.setOffering(offeringID);
                            req.getObservables().add(obsProp);
                            req.setTime(TimeExtent.getPeriodStartingNow(STREAM_END_TIME));
                            req.setXmlWrapper(false);
                            
                            // create client and retrieve result template
                            SOSClient sos = new SOSClient(req, config.sosUseWebsockets);
                            sosClients.add(sos);
                            sos.retrieveStreamDescription();
                            DataComponent recordDef = sos.getRecordDescription();
                            if (recordDef.getName() == null)
                                recordDef.setName("output" + outputNum);
                            
                            // retrieve sensor description from remote SOS if available (first time only)
                            try
                            {
                                if (outputNum == 1 && config.sensorML == null)
                                    this.sensorDescription = (AbstractPhysicalProcess)sos.getSensorDescription(config.sensorUID);
                            }
                            catch (SensorHubException e)
                            {
                                log.warn(e.getMessage());
                            }
                            
                            // create output
                            final SWEVirtualSensorOutput output = new SWEVirtualSensorOutput(this, recordDef, sos.getRecommendedEncoding());
                            this.addOutput(output, false);
                            
                            sos.startStream(new SOSRecordListener() {
                                @Override
                                public void newRecord(DataBlock data)
                                {
                                    output.publishNewRecord(data);
                                }
                            });
                            
                            outputNum++;
                        }
                    }
                }
            }
            
            if (sosClients.isEmpty())
                throw new SensorHubException("Requested observation data is not available from SOS " + config.sosEndpointUrl +
                                             ". Check Sensor UID and observed properties have valid values." );
        }
        
        // create and start SPS client
        if (config.spsEndpointUrl != null)
        {
            spsClient = new SPSClient(config.spsEndpointUrl, SPS_VERSION, config.sensorUID);
            spsClient.retrieveCommandDescription();
            DataComponent cmdDef = spsClient.getCommandDescription();
            if (cmdDef instanceof DataChoice)
            {
                int choiceIndex = 0;
                for (DataComponent item: ((DataChoice) cmdDef).getItemList())
                {
                    SWEVirtualSensorControl controlInput = new SWEVirtualSensorControl(this, item, choiceIndex);
                    this.addControlInput(controlInput);
                    choiceIndex++;
                }
            }
            else
            {
                if (cmdDef.getName() == null)
                    cmdDef.setName("command");
                SWEVirtualSensorControl controlInput = new SWEVirtualSensorControl(this, cmdDef);
                this.addControlInput(controlInput);
            }
        }           
    }


    @Override
    public void stop() throws SensorHubException
    {
        // stop all SOS streams
        if (sosClients != null)
        {
            for (SOSClient sos: sosClients)
                sos.stopStream();

            sosClients = null;
        }
    }
    
    
    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            super.updateSensorDescription();
            
            // if no sensor description was obtained from remote SOS, just set identifier
            sensorDescription.setId("SWE_SENSOR");
            sensorDescription.setUniqueIdentifier(config.sensorUID);
        }
    }


    @Override
    public AbstractFeature getCurrentFeatureOfInterest()
    {
        return currentFoi;
    }


    @Override
    public void cleanup() throws SensorHubException
    {

    }


    @Override
    public boolean isConnected()
    {
        return false;
    }
}
