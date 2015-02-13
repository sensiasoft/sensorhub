/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.android;

import java.util.LinkedHashMap;
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
import org.vast.ogc.om.IObservation;
import org.vast.ogc.om.ObservationImpl;
import org.vast.ows.OWSException;
import org.vast.ows.sos.InsertResultRequest;
import org.vast.ows.sos.InsertResultTemplateRequest;
import org.vast.ows.sos.InsertResultTemplateResponse;
import org.vast.ows.sos.InsertSensorRequest;
import org.vast.ows.sos.SOSUtils;
import org.vast.ows.swe.InsertSensorResponse;
import org.vast.swe.SWEData;


public class SOSTClient implements IEventListener
{
    protected static final Logger log = LoggerFactory.getLogger(SOSTClient.class);
   
    SOSUtils sosUtils = new SOSUtils();
    ExecutorService threadPool;
    String endPoint;
    String offering;
    Map<ISensorDataInterface, StreamInfo> dataStreams;
    
    
    public class StreamInfo
    {
        String templateID;
        SWEData resultData = new SWEData();
        int minRecordsPerRequest = 10;
        long lastSampleTime = -1;
        int errorCount = 0;
    }
    
    
    public SOSTClient(String endPoint)
    {
        this.endPoint = endPoint;
        this.threadPool = Executors.newFixedThreadPool(4);
        this.dataStreams = new LinkedHashMap<ISensorDataInterface, StreamInfo>();
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
            // build insert sensor request
            InsertSensorRequest req = new InsertSensorRequest();
            req.setPostServer(endPoint);
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
                                req.setPostServer(endPoint);
                                req.setVersion("2.0");
                                req.setTemplateId(streamInfo.templateID);
                                req.setResultData(streamInfo.resultData);
                                
                                synchronized(streamInfo.resultData)
                                {
                                    sosUtils.sendRequest(req, false);
                                    //sosUtils.writeXMLQuery(System.out, req);
                                    
                                    // clear everything that was sent
                                    streamInfo.resultData.clearData();
                                }
                            }
                            catch (OWSException ex)
                            {
                                log.error("Error when sending data to SOS-T: " + ((SensorDataEvent)e).getSource().getName(), ex);
                                streamInfo.errorCount++;
                            }
                        }           
                    };
                    
                   //threadPool.execute(sendTask);
                   sendTask.run();
                }
            }
        }
    }
    
    
    public void stop()
    {
        for (ISensorDataInterface output: dataStreams.keySet())
            output.unregisterListener(this);
        threadPool.shutdown();
    }
    
    
    public boolean isConnected()
    {
        return (offering != null);
    }
    
    
    public Map<ISensorDataInterface, StreamInfo> getDataStreams()
    {
        return dataStreams;
    }
}
