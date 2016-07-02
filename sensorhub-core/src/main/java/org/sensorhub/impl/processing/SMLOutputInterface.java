/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.processing;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.processing.ProcessException;
import org.sensorhub.impl.SensorHub;
import org.vast.process.DataQueue;
import org.vast.swe.SWEHelper;


/*
 * Implementation of streaming data interface that forwards data obtained from 
 * SensorML process output data queues as SensorHub DataEvents
 */
class SMLOutputInterface implements IStreamingDataInterface
{
    SMLStreamProcess parentProcess;
    IEventHandler eventHandler;
    DataComponent outputDef;
    DataEncoding outputEncoding;
    DataBlock lastRecord;
    long lastRecordTime = Long.MIN_VALUE;
    double avgSamplingPeriod = 1.0;
    int avgSampleCount = 0;
    
    
    protected DataQueue outputQueue = new DataQueue()
    {
        @Override
        public synchronized void add(DataBlock data)
        {
            long now = System.currentTimeMillis();
            double timeStamp = now / 1000.;
            
            // refine sampling period
            if (!Double.isNaN(lastRecordTime))
            {
                double dT = timeStamp - lastRecordTime;
                avgSampleCount++;
                if (avgSampleCount == 1)
                    avgSamplingPeriod = dT;
                else
                    avgSamplingPeriod += (dT - avgSamplingPeriod) / avgSampleCount; 
            }
            
            // save last record and send event
            lastRecord = data;
            lastRecordTime = now;
            DataEvent e = new DataEvent(now, SMLOutputInterface.this, data);
            eventHandler.publishEvent(e);
        }        
    };
    

    protected SMLOutputInterface(SMLStreamProcess parentProcess, DataComponent outputDef) throws ProcessException
    {
        this.parentProcess = parentProcess;
        this.outputDef = outputDef;
        this.outputEncoding = SWEHelper.getDefaultEncoding(outputDef);
        
        try
        {
            parentProcess.smlProcess.connectOutput(outputDef.getName(), "/", outputQueue);
        }
        catch (org.vast.process.SMLException e)
        {
            throw new ProcessException("Error while connecting output " + outputDef.getName(), e);
        }
        
        // obtain an event handler for this output
        String moduleID = parentProcess.getLocalID();
        String topic = getName();
        this.eventHandler = SensorHub.getInstance().getEventBus().registerProducer(moduleID, topic);
    }
    

    @Override
    public IDataProducerModule<?> getParentModule()
    {
        return parentProcess;
    }


    @Override
    public String getName()
    {
        return outputDef.getName();
    }


    @Override
    public boolean isEnabled()
    {
        return true;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return outputDef;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return outputEncoding;
    }


    @Override
    public DataBlock getLatestRecord()
    {
        return lastRecord;
    }


    @Override
    public long getLatestRecordTime()
    {
        return lastRecordTime;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return avgSamplingPeriod;
    }
    
    
    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }

}
