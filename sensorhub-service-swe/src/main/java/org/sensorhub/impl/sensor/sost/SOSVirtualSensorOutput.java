/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.sost;

import net.opengis.gml.v32.AbstractFeature;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.FoiEvent;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.ogc.gml.FeatureRef;


public class SOSVirtualSensorOutput extends AbstractSensorOutput<SOSVirtualSensor>
{
    SOSVirtualSensor parentSensor;
    DataComponent recordStructure;
    DataEncoding recordEncoding;
    double avgSamplingPeriod = 100;
    int avgSampleCount = 0;
    
    
    public SOSVirtualSensorOutput(SOSVirtualSensor sensor, DataComponent recordStructure, DataEncoding recordEncoding)
    {
        super(sensor);
        this.recordStructure = recordStructure;
        this.recordEncoding = recordEncoding;
        
        // force raw binary encoding (no reason to recommend base64)
        // switching to base64 is automatic when writing or parsing from XML
        if (recordEncoding instanceof BinaryEncoding)
            ((BinaryEncoding) recordEncoding).setByteEncoding(ByteEncoding.RAW);
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
    
    
    public void publishNewRecord(DataBlock dataBlock)
    {
        long now = System.currentTimeMillis();
        updateSamplingPeriod(now);
        
        // publish new sensor data event
        latestRecord = dataBlock;
        latestRecordTime = now;
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, this, dataBlock));
    }
    
    
    public void publishNewFeatureOfInterest(AbstractFeature foi)
    {
        if (foi != null)
        {            
            long now = System.currentTimeMillis();
            FoiEvent e = null;
            
            if (foi instanceof FeatureRef)
            {
                try
                {
                    foi = ((FeatureRef) foi).getTarget();
                    e = new FoiEvent(now, getParentModule(), foi, now/1000.0);
                }
                catch (Exception e1)
                {
                    e = new FoiEvent(now, getParentModule(), ((FeatureRef) foi).getHref(), now/1000.0);
                }                    
            }
            else
            {
                e = new FoiEvent(now, getParentModule(), foi, now/1000.0);
            }

            eventHandler.publishEvent(e);
        }
    }
    
    
    /*
     * Refine sampling period at each new measure received by 
     * incrementally computing dt average for the 100 first records
     */
    protected void updateSamplingPeriod(long timeStamp)
    {
        if (latestRecordTime == Long.MIN_VALUE)
            return;
                
        if (avgSampleCount < 100)
        {
            if (avgSampleCount == 0)
                avgSamplingPeriod = 0.0;
            else
                avgSamplingPeriod *= (double)avgSampleCount / (avgSampleCount+1);
            
            avgSampleCount++;
            avgSamplingPeriod += (timeStamp - latestRecordTime) / 1000.0 / avgSampleCount;
            
            SOSVirtualSensor.log.trace("Sampling period = " + avgSamplingPeriod + "s");
        }
    }
    
}
