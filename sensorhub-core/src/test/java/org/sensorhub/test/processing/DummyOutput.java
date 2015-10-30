/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.processing;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.processing.IStreamProcessModule;
import org.sensorhub.impl.common.BasicEventHandler;
import org.vast.swe.SWEHelper;


class DummyOutput implements IStreamingDataInterface
{
    IStreamProcessModule<?> parentProcess;
    IEventHandler eventHandler;
    DataComponent outputDef;
    DataEncoding outputEncoding;
    DataBlock lastRecord;
    long lastRecordTime = Long.MIN_VALUE;
    double avgSamplingPeriod = 1.0;
    int avgSampleCount = 0;
        

    protected DummyOutput(IStreamProcessModule<?> parentProcess, DataComponent outputDef)
    {
        this.parentProcess = parentProcess;
        this.outputDef = outputDef;
        this.outputEncoding = SWEHelper.getDefaultEncoding(outputDef);
        this.eventHandler = new BasicEventHandler();
    }
    
    
    public void sendOutput(DataBlock dataBlk)
    {
        lastRecord = dataBlk;
        lastRecordTime = System.currentTimeMillis();
        DataEvent outputEvent = new DataEvent(lastRecordTime, this, dataBlk);
        eventHandler.publishEvent(outputEvent);
    }
    

    @Override
    public IStreamProcessModule<?> getParentModule()
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
