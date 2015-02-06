/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.client.sost;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ogc.om.IObservation;
import org.vast.ogc.om.ObservationImpl;
import org.vast.ows.OWSException;
import org.vast.ows.sos.InsertResultRequest;
import org.vast.ows.sos.InsertResultTemplateRequest;
import org.vast.ows.sos.InsertResultTemplateResponse;
import org.vast.ows.sos.InsertSensorRequest;
import org.vast.ows.sos.SOSUtils;
import org.vast.ows.swe.InsertSensorResponse;
import org.vast.ows.swe.UpdateSensorRequest;
import org.vast.swe.SWEData;


/**
 * <p>
 * Implementation of an SOS-T client that listens to sensor events and 
 * forwards them to an SOS via InsertSensor/UpdateSensor, InsertResultTemplate
 * and InsertResult requests.<br/>
 * </p>
 *
 * <p>Copyright (c) 2015</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Feb 6, 2015
 */
public class SOSTClient extends AbstractModule<SOSTClientConfig> implements IEventListener
{
    protected static final Logger log = LoggerFactory.getLogger(SOSTClient.class);
   
    ISensorModule<?> sensor;
    SOSUtils sosUtils = new SOSUtils();    
    String offering;
    Map<ISensorDataInterface, StreamInfo> dataStreams;
    
    
    protected class StreamInfo
    {
        String templateID;
        SWEData resultData = new SWEData();
        int minRecordsPerRequest = 10;
        long lastSampleTime = -1;
        int errorCount = 0;
        ThreadPoolExecutor threadPool;
    }
    
    
    public SOSTClient()
    {
        this.dataStreams = new LinkedHashMap<ISensorDataInterface, StreamInfo>();
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        this.sensor = SensorHub.getInstance().getSensorManager().getModuleById(config.sensorID);
                
        try
        {
            // register sensor
            registerSensor(sensor);
            log.info("Sensor " + MsgUtils.moduleString(sensor) + " registered with SOS");
            
            // register all templates
            for (ISensorDataInterface o: sensor.getAllOutputs().values())
                registerDataStream(o);
            log.info("Result templates registered with SOS");
        }
        catch (Exception e)
        {
            throw new ServiceException("Error while registering sensor with remote SOS", e);
        }
    }
    
    
    @Override
    public void stop() throws SensorHubException
    {
        // unregister from sensor
        if (sensor != null)
            sensor.unregisterListener(this);
        
        // unregister from output data events and stop all threads
        for (Entry<ISensorDataInterface, StreamInfo> entry: dataStreams.entrySet())
        {
            entry.getKey().unregisterListener(this);
            entry.getValue().threadPool.shutdown();
        }
    }
    
    
    /**
     * Registers sensor with remote SOS
     * @param sensor
     * @throws OWSException
     */
    protected void registerSensor(ISensorModule<?> sensor) throws OWSException
    {
        try
        {
            // build insert sensor request
            InsertSensorRequest req = new InsertSensorRequest();
            req.setPostServer(config.sosEndpointUrl);
            req.setVersion("2.0");
            req.setProcedureDescription(sensor.getCurrentSensorDescription());
            req.setProcedureDescriptionFormat(InsertSensorRequest.DEFAULT_PROCEDURE_FORMAT);
            req.getObservationTypes().add(IObservation.OBS_TYPE_RECORD);
            req.getFoiTypes().add("gml:Feature");
            
            InsertSensorResponse resp = (InsertSensorResponse)sosUtils.sendRequest(req, false);
            this.offering = resp.getAssignedOffering();
        }
        catch (SensorException e)
        {
            throw new RuntimeException("Cannot get SensorML description for sensor " + sensor.getName());
        }
        
        // register to sensor change event
        sensor.registerListener(this);
    }
    
    
    /**
     * Update sensor description at remote SOS
     * @param sensor
     * @throws OWSException
     */
    protected void updateSensor(ISensorModule<?> sensor) throws OWSException
    {
        try
        {
            // build update sensor request
            UpdateSensorRequest req = new UpdateSensorRequest(SOSUtils.SOS);
            req.setPostServer(config.sosEndpointUrl);
            req.setVersion("2.0");
            req.setProcedureId(sensor.getCurrentSensorDescription().getUniqueIdentifier());
            req.setProcedureDescription(sensor.getCurrentSensorDescription());
            req.setProcedureDescriptionFormat(InsertSensorRequest.DEFAULT_PROCEDURE_FORMAT);
            
            sosUtils.sendRequest(req, false);
        }
        catch (SensorException e)
        {
            throw new RuntimeException("Cannot get SensorML description for sensor " + sensor.getName());
        }
    }
    
    
    /**
     * Prepare to send the given sensor output data to the remote SOS server
     * @param sensorOutput
     * @throws OWSException 
     */
    protected void registerDataStream(ISensorDataInterface sensorOutput) throws OWSException
    {
        // send insert result template
        InsertResultTemplateRequest req = new InsertResultTemplateRequest();
        req.setPostServer(config.sosEndpointUrl);
        req.setVersion("2.0");
        req.setOffering(offering);
        req.setResultStructure(sensorOutput.getRecordDescription());
        req.setResultEncoding(sensorOutput.getRecommendedEncoding());
        req.setObservationTemplate(new ObservationImpl());
        InsertResultTemplateResponse resp = (InsertResultTemplateResponse)sosUtils.sendRequest(req, false);
        
        // if binary encoding, enforce base64
        DataEncoding dataEnc = sensorOutput.getRecommendedEncoding();
        if (dataEnc instanceof BinaryEncoding)
        {
            dataEnc = dataEnc.copy();
            ((BinaryEncoding) dataEnc).setByteEncoding(ByteEncoding.BASE_64);
        }
        
        // add stream info to map
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.templateID = resp.getAcceptedTemplateId();
        streamInfo.resultData.setElementType(sensorOutput.getRecordDescription());
        streamInfo.resultData.setEncoding(dataEnc);
        streamInfo.minRecordsPerRequest = 1;//(int)(1.0 / sensorOutput.getAverageSamplingPeriod());
        dataStreams.put(sensorOutput, streamInfo);
        
        // start thread pool
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(2);
        streamInfo.threadPool = new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, workQueue);
        
        // register to data events
        sensorOutput.registerListener(this);
    }
    
    
    @Override
    public void handleEvent(final Event e)
    {
        // sensor description updated
        if (e instanceof SensorEvent)
        {
            if (((SensorEvent) e).getType() == SensorEvent.Type.SENSOR_CHANGED)
            {
                try
                {
                    updateSensor(sensor);
                }
                catch (OWSException ex)
                {
                    log.error("Error when sending updates sensor description to SOS-T", ex);
                }
            }
        }
        
        // sensor data received
        if (e instanceof SensorDataEvent)
        {
            // retrieve stream info
            final StreamInfo streamInfo = dataStreams.get(e.getSource());
            if (streamInfo == null)
                return;
            
            // record last sample time
            streamInfo.lastSampleTime = e.getTimeStamp();
            
            // append records to buffer
            synchronized(streamInfo.resultData)
            {
                for (DataBlock record: ((SensorDataEvent)e).getRecords())
                    streamInfo.resultData.pushNextDataBlock(record);
                        
                if (streamInfo.resultData.getNumElements() >= streamInfo.minRecordsPerRequest)
                {
                    Runnable sendTask = new Runnable() {
                        @Override
                        public void run()
                        {
                            try
                            {
                                InsertResultRequest req = new InsertResultRequest();
                                req.setPostServer(config.sosEndpointUrl);
                                req.setVersion("2.0");
                                req.setTemplateId(streamInfo.templateID);
                                req.setResultData(streamInfo.resultData);
                                
                                synchronized(streamInfo.resultData)
                                {
                                    //sosUtils.writeXMLQuery(System.out, req);
                                    sosUtils.sendRequest(req, false);
                                    
                                    // clear everything that was sent
                                    streamInfo.resultData.clearData();
                                }
                            }
                            catch (OWSException ex)
                            {
                                String outputName = ((SensorDataEvent)e).getSource().getName();
                                log.error("Error when sending '" + outputName + "' data to SOS-T from " + MsgUtils.moduleString(sensor), ex);
                                streamInfo.errorCount++;
                            }
                        }           
                    };
                    
                    try
                    {
                        streamInfo.threadPool.execute(sendTask);
                    }
                    catch (RejectedExecutionException ex)
                    {
                        String outputName = ((SensorDataEvent)e).getSource().getName();
                        log.error("Too many requests to SOS-T for '" + outputName + "' of " + MsgUtils.moduleString(sensor) + ". Bandwidth cannot keep up.");
                        streamInfo.errorCount++;
                    }
                }
            }
        }
    }
    
    
    public boolean isConnected()
    {
        return (offering != null);
    }
    
    
    public Map<ISensorDataInterface, StreamInfo> getDataStreams()
    {
        return dataStreams;
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        
    }
}
