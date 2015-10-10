/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor;

import org.sensorhub.api.sensor.ISensorModule;


/**
 * <p>
 * Specialized output class for variable rate sensors.<br/>
 * This provides facility to compute the average sampling rate on the fly.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <SensorType> Type of parent sensor
 * @since Sep 5, 2015
 */
public abstract class VarRateSensorOutput<SensorType extends ISensorModule<?>> extends AbstractSensorOutput<SensorType>
{
    double avgSamplingPeriod = 10.0;
    int avgSampleCount = 0;
    
    
    public VarRateSensorOutput(SensorType parentSensor, double initialSamplingPeriod)
    {
        this(null, parentSensor,initialSamplingPeriod);
    }
    
    
    public VarRateSensorOutput(String name, SensorType parentSensor, double initialSamplingPeriod)
    {
        super(name, parentSensor);
        this.avgSamplingPeriod = initialSamplingPeriod;
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
            
            //log.trace("Sampling period = " + avgSamplingPeriod + "s");
        }
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return avgSamplingPeriod;
    }
}
