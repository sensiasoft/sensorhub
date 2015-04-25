/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.client.sost;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
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
import org.vast.cdm.common.DataStreamWriter;
import org.vast.data.DataBlockList;
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
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 6, 2015
 */
public class SOSTClient extends AbstractModule<SOSTClientConfig> implements IEventListener
{
    protected static final Logger log = LoggerFactory.getLogger(SOSTClient.class);
   
    ISensorModule<?> sensor;
    SOSUtils sosUtils = new SOSUtils();    
    String offering;
    Map<ISensorDataInterface, StreamInfo> dataStreams;
    
    
    public class StreamInfo
    {
        String templateID;
        public long lastEventTime = -1;
        public int errorCount = 0;
        private int minRecordsPerRequest = 10;
        private SWEData resultData = new SWEData();
        private ThreadPoolExecutor threadPool;
        private DataStreamWriter persistentWriter;
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
        
        // cleanup all streams
        for (Entry<ISensorDataInterface, StreamInfo> entry: dataStreams.entrySet())
            stopStream(entry.getKey(), entry.getValue());
    }
    
    
    protected void stopStream(ISensorDataInterface output, StreamInfo streamInfo)
    {
        // unregister listeners
        output.unregisterListener(this);
        
        // stop threads
        streamInfo.threadPool.shutdown();
        
        // close open HTTP streams
        try
        {
            if (streamInfo.persistentWriter != null)
                streamInfo.persistentWriter.close();
        }
        catch (IOException e)
        {
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
            req.setProcedureDescription(sensor.getCurrentDescription());
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
            req.setProcedureId(sensor.getCurrentDescription().getUniqueIdentifier());
            req.setProcedureDescription(sensor.getCurrentDescription());
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
        
        // add stream info to map
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.templateID = resp.getAcceptedTemplateId();
        streamInfo.resultData.setElementType(sensorOutput.getRecordDescription());
        streamInfo.resultData.setEncoding(sensorOutput.getRecommendedEncoding());
        streamInfo.minRecordsPerRequest = 1;//(int)(1.0 / sensorOutput.getAverageSamplingPeriod());
        dataStreams.put(sensorOutput, streamInfo);
        
        // start thread pool
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(2);
        streamInfo.threadPool = new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, workQueue);
        
        // register to data events
        sensorOutput.registerListener(this);
    }
    
    
    @Override
    public void handleEvent(final Event<?> e)
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
        
        // data received
        else if (e instanceof DataEvent)
        {
            // retrieve stream info
            StreamInfo streamInfo = dataStreams.get(e.getSource());
            if (streamInfo == null)
                return;
            
            // we stop here if we had too many errors
            if (streamInfo.errorCount >= config.maxConnectErrors)
            {
                String outputName = ((SensorDataEvent)e).getSource().getName();
                log.error("Too many errors sending '" + outputName + "' data to SOS-T from " + MsgUtils.moduleString(sensor) + ". Stopping Stream.");
                stopStream((ISensorDataInterface)e.getSource(), streamInfo);
                return;
            }
            
            // skip if we cannot handle more requests
            if (streamInfo.threadPool.getQueue().remainingCapacity() == 0)
            {
                String outputName = ((SensorDataEvent)e).getSource().getName();
                if (log.isDebugEnabled())
                    log.debug("Too many requests to SOS-T for '" + outputName + "' of " + MsgUtils.moduleString(sensor) + ". Bandwidth cannot keep up.");
                return;
            }
            
            // record last event time
            streamInfo.lastEventTime = e.getTimeStamp();
            
            // append records to buffer
            for (DataBlock record: ((SensorDataEvent)e).getRecords())
                streamInfo.resultData.pushNextDataBlock(record);
            
            // send record using one of 2 methods
            if (config.usePersistentConnection)
                sendInPersistentRequest((SensorDataEvent)e, streamInfo);
            else
                sendAsNewRequest((SensorDataEvent)e, streamInfo);
        }
    }
    
    
    private void sendAsNewRequest(final SensorDataEvent e, final StreamInfo streamInfo)
    {
        // send request if min record count is reached
        if (streamInfo.resultData.getNumElements() >= streamInfo.minRecordsPerRequest)
        {
            final InsertResultRequest req = new InsertResultRequest();
            req.setPostServer(config.sosEndpointUrl);
            req.setVersion("2.0");
            req.setTemplateId(streamInfo.templateID);
            req.setResultData(streamInfo.resultData);
            
            // create new container for future data
            streamInfo.resultData = streamInfo.resultData.copy();
            
            // create send request task
            Runnable sendTask = new Runnable() {
                @Override
                public void run()
                {
                    try
                    {
                        if (log.isDebugEnabled())
                        {
                            String outputName = e.getSource().getName();
                            log.debug("Sending '" + outputName + "' record(s) to SOS-T");
                        }
                        
                        //sosUtils.writeXMLQuery(System.out, req);
                        sosUtils.sendRequest(req, false);
                    }
                    catch (Exception ex)
                    {
                        String outputName = e.getSource().getName();
                        log.error("Error when sending '" + outputName + "' data to SOS-T from " + MsgUtils.moduleString(sensor), ex);
                        streamInfo.errorCount++;
                    }
                }           
            };
            
            // run task in async thread pool
            streamInfo.threadPool.execute(sendTask);
        }
    }
    
    
    private void sendInPersistentRequest(final SensorDataEvent e, final StreamInfo streamInfo)
    {
        // create new container for future data
        final DataBlockList dataBlockList = (DataBlockList)streamInfo.resultData.getData();
        streamInfo.resultData.clearData();
        
        // create send request task
        Runnable sendTask = new Runnable() {
            @Override
            public void run()
            {
                try
                {
                    // connect if not already connected
                    if (streamInfo.persistentWriter == null)
                    {                        
                        if (log.isDebugEnabled())
                            log.debug("Connecting to " + config.sosEndpointUrl + "...");
                        
                        final InsertResultRequest req = new InsertResultRequest();
                        req.setPostServer(config.sosEndpointUrl);
                        req.setVersion("2.0");
                        req.setTemplateId(streamInfo.templateID);
                        
                        // connect to server                        
                        HttpURLConnection conn = sosUtils.sendPostRequestWithQuery(req);                        
                        conn.setRequestProperty("Content-type", "text/plain");
                        conn.setChunkedStreamingMode(32);
                        conn.connect();
                        
                        // prepare writer
                        streamInfo.persistentWriter = streamInfo.resultData.getDataWriter();
                        streamInfo.persistentWriter.setOutput(new BufferedOutputStream(conn.getOutputStream()));
                    }
                    
                    // write record to output stream
                    Iterator<DataBlock> it = dataBlockList.blockIterator();
                    while (it.hasNext())
                    {
                        if (log.isDebugEnabled())
                        {
                            String outputName = e.getSource().getName();
                            log.debug("Sending '" + outputName + "' record(s) to SOS-T");
                        }
                        
                        streamInfo.persistentWriter.write(it.next());
                    }
                    
                    streamInfo.persistentWriter.flush();
                }
                catch (Exception ex)
                {
                    String outputName = e.getSource().getName();
                    log.error("Error when sending '" + outputName + "' data to SOS-T from " + MsgUtils.moduleString(sensor), ex);
                    streamInfo.errorCount++;
                    
                    try
                    {
                        if (streamInfo.persistentWriter != null)
                            streamInfo.persistentWriter.close();
                    }
                    catch (IOException e1)
                    {
                    }
                    
                    streamInfo.persistentWriter = null;
                    
                    // wait a little before trying to reconnect
                    log.debug("Waiting to reconnect...");
                    try { Thread.sleep(3000L); }
                    catch (InterruptedException e1) { }
                }
            }           
        };
        
        // run task in async thread pool
        streamInfo.threadPool.execute(sendTask);
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
