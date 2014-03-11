/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.sost;

import java.util.List;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorInterface;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.cdm.common.DataEncoding;


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
    public ISensorInterface<?> getSensorInterface()
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
