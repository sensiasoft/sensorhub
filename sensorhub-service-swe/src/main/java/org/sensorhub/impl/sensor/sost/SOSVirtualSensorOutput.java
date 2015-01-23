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

package org.sensorhub.impl.sensor.sost;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;


public class SOSVirtualSensorOutput extends AbstractSensorOutput<SOSVirtualSensor>
{
    SOSVirtualSensor parentSensor;
    DataComponent recordStructure;
    DataEncoding recordEncoding;
    DataBlock latestRecord;
    double lastRecordTime = Double.NEGATIVE_INFINITY;
    double avgSamplingPeriod = 100;
    int avgSampleCount = 0;
    
    
    public SOSVirtualSensorOutput(SOSVirtualSensor sensor, DataComponent recordStructure, DataEncoding recordEncoding)
    {
        super(sensor);
        this.recordStructure = recordStructure;
        this.recordEncoding = recordEncoding;
    }


    @Override
    protected void init() throws SensorException
    {
        
    }


    @Override
    public String getName()
    {
        return recordStructure.getName();
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return avgSamplingPeriod;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return recordStructure;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return recordEncoding;
    }


    @Override
    public DataBlock getLatestRecord() throws SensorException
    {
        return latestRecord;
    }
    
    
    @Override
    public double getLatestRecordTime()
    {
        return lastRecordTime;
    }
    
    
    public void publishNewRecord(DataBlock dataBlock)
    {
        // TODO obtain sampling time from record when possible 
        double timeStamp =  System.currentTimeMillis() / 1000.;      
        updateSamplingPeriod(timeStamp);       
                
        // publish new sensor data event
        latestRecord = dataBlock;
        lastRecordTime = timeStamp;
        eventHandler.publishEvent(new SensorDataEvent(lastRecordTime, this, dataBlock));
    }
    
    
    /*
     * Refine sampling period at each new measure received by 
     * incrementally computing dt average for the 100 first records
     */
    protected void updateSamplingPeriod(double timeStamp)
    {
        if (lastRecordTime < 0)
            return;
        
        if (avgSampleCount < 100)
        {
            if (avgSampleCount == 0)
                avgSamplingPeriod = 0.0;
            else
                avgSamplingPeriod *= (double)avgSampleCount / (avgSampleCount+1);
            avgSampleCount++;
            avgSamplingPeriod += (timeStamp - lastRecordTime) / avgSampleCount;
        }
    }
    
}
