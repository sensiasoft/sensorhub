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

import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.VarRateSensorOutput;


public class SWEVirtualSensorOutput extends VarRateSensorOutput<SWEVirtualSensor>
{
    SWEVirtualSensor parentSensor;
    DataComponent recordStructure;
    DataEncoding recordEncoding;
    
    
    public SWEVirtualSensorOutput(SWEVirtualSensor sensor, DataComponent recordStructure, DataEncoding recordEncoding)
    {
        super(recordStructure.getName(), sensor, 1.0);
        this.recordStructure = recordStructure;
        this.recordEncoding = recordEncoding;
        
        // force raw binary encoding (no reason to recommend base64)
        // switching to base64 is automatic when writing or parsing from XML
        if (recordEncoding instanceof BinaryEncoding)
            ((BinaryEncoding) recordEncoding).setByteEncoding(ByteEncoding.RAW);
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
}
