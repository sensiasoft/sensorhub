/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.android;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ows.OWSException;
import org.vast.ows.sos.InsertResultRequest;
import org.vast.ows.sos.InsertResultTemplateRequest;
import org.vast.ows.sos.InsertResultTemplateResponse;
import org.vast.ows.sos.InsertSensorRequest;
import org.vast.ows.sos.InsertSensorResponse;
import org.vast.ows.sos.SOSUtils;
import org.vast.swe.SWEData;


public class SOSTClient implements IEventListener
{
    protected static final Logger log = LoggerFactory.getLogger(SOSTClient.class);
   
    SOSUtils sosUtils = new SOSUtils();
    ExecutorService threadPool;
    String endPoint;
    String offering;
    Map<ISensorDataInterface, StreamInfo> dataStreams;
    int numRecordPerRequest;
    
    
    class StreamInfo
    {
        String templateID;
        SWEData resultData = new SWEData();
        int minRecordsPerRequest = 10;
    }
    
    
    public SOSTClient(String sosEndpoint)
    {
        threadPool = Executors.newFixedThreadPool(4);
        dataStreams = new HashMap<ISensorDataInterface, StreamInfo>();
    }
    
    
    /**
     * Registers sensor with remote SOS
     * @param sensor
     * @throws OWSException
     */
    public void registerSensor(ISensorModule<?> sensor) throws OWSException
    {
        try
        {
            // register sensor
            InsertSensorRequest req = new InsertSensorRequest();
            req.setPostServer(endPoint);
            req.setProcedureDescription(sensor.getCurrentSensorDescription());
            req.setProcedureDescriptionFormat(InsertSensorRequest.DEFAULT_PROCEDURE_FORMAT);
            InsertSensorResponse resp = (InsertSensorResponse)sosUtils.sendRequest(req, false);
            this.offering = resp.getAssignedOffering();
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
    public void registerDataStream(ISensorDataInterface sensorOutput) throws OWSException
    {
        // send insert result template
        InsertResultTemplateRequest req = new InsertResultTemplateRequest();
        req.setPostServer(endPoint);
        req.setOffering(offering);
        req.setResultStructure(sensorOutput.getRecordDescription());
        req.setResultEncoding(sensorOutput.getRecommendedEncoding());
        InsertResultTemplateResponse resp = (InsertResultTemplateResponse)sosUtils.sendRequest(req, false);
        
        // add stream info to map
        StreamInfo streamInfo = new StreamInfo();
        streamInfo.templateID = resp.getAcceptedTemplateId();
        streamInfo.resultData.setElementType(sensorOutput.getRecordDescription());
        streamInfo.resultData.setEncoding(sensorOutput.getRecommendedEncoding());
        streamInfo.minRecordsPerRequest = (int)(1.0 / sensorOutput.getAverageSamplingPeriod());
        dataStreams.put(sensorOutput, streamInfo);
        
        // register to data events
        sensorOutput.registerListener(this);
    }
    
    
    @Override
    public void handleEvent(final Event e)
    {
        if (e instanceof SensorDataEvent)
        {
            // retrieve stream info
            final StreamInfo streamInfo = dataStreams.get(e.getSource());
            if (streamInfo == null)
                return;
            
            // append records to buffer
            for (DataBlock record: ((SensorDataEvent)e).getRecords())
                streamInfo.resultData.pushNextDataBlock(record);
            
            if (streamInfo.resultData.getComponentCount() >= streamInfo.minRecordsPerRequest)
            {
                threadPool.execute(new Runnable() {
                    @Override
                    public void run()
                    {
                        try
                        {
                            InsertResultRequest req = new InsertResultRequest();
                            req.setPostServer(endPoint);
                            req.setTemplateId(streamInfo.templateID);                        
                            req.setResultData(streamInfo.resultData);
                            sosUtils.sendRequest(req, false);
                            
                            // clear everything that was sent
                            streamInfo.resultData.clearData();
                        }
                        catch (OWSException e1)
                        {
                            log.error("Error when sending data to SOS-T: " + ((SensorDataEvent)e).getSource().getName());
                        }
                    }            
                });
            }
        }
    }

}
