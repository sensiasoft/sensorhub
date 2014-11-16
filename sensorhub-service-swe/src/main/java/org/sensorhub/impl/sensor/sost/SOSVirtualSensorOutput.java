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

import java.util.List;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;


public class SOSVirtualSensorOutput implements ISensorDataInterface
{
    SOSVirtualSensor parentSensor;
    IEventHandler eventHandler;
    DataComponent recordStructure;
    DataEncoding recordEncoding;
    DataBlock latestRecord;
    
    
    public SOSVirtualSensorOutput(SOSVirtualSensor sensor, DataComponent recordStructure, DataEncoding recordEncoding)
    {
        this.parentSensor = sensor;
        this.recordStructure = recordStructure;
        this.recordEncoding = recordEncoding;
        this.eventHandler = new BasicEventHandler();
    }
    
    
    @Override
    public boolean isEnabled()
    {
        return true;
    }
    
    
    @Override
    public boolean isStorageSupported()
    {
        return false;
    }


    @Override
    public boolean isPushSupported()
    {
        return true;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return Double.NaN;
    }


    @Override
    public DataComponent getRecordDescription() throws SensorException
    {
        return recordStructure;
    }


    @Override
    public DataEncoding getRecommendedEncoding() throws SensorException
    {
        return recordEncoding;
    }


    @Override
    public DataBlock getLatestRecord() throws SensorException
    {
        return latestRecord;
    }


    @Override
    public int getStorageCapacity() throws SensorException
    {
        return 0;
    }


    @Override
    public int getNumberOfAvailableRecords() throws SensorException
    {
        return 1;
    }


    @Override
    public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException
    {
        return null;
    }


    @Override
    public List<DataBlock> getAllRecords(boolean clear) throws SensorException
    {
        return null;
    }


    @Override
    public int clearAllRecords() throws SensorException
    {
        latestRecord = null;
        return 0;
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


    @Override
    public ISensorModule<?> getSensorInterface()
    {
        return parentSensor;
    }
    
    
    public void publishNewRecord(DataBlock dataBlock)
    {
        try
        {
            latestRecord = dataBlock;
            eventHandler.publishEvent(new SensorDataEvent(this, System.currentTimeMillis(), getRecordDescription(), dataBlock));
        }
        catch (SensorException e)
        {
            e.printStackTrace();
        }
    }
    
}
