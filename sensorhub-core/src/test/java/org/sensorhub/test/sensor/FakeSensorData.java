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

package org.sensorhub.test.sensor;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Time;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.vast.data.DataBlockDouble;
import org.vast.data.DataRecordImpl;
import org.vast.data.QuantityImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.data.TimeImpl;


/**
 * <p>
 * Fake sensor implementation for testing SOS service
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 20, 2013
 */
public class FakeSensorData extends AbstractSensorOutput<FakeSensor>
{
    String name;
    boolean pushEnabled;
    int maxSampleCount;
    int count;
    int bufferSize;
    double samplingPeriod; // seconds
    Deque<DataBlock> dataQueue;
    
    
    public FakeSensorData(FakeSensor sensor, String name, boolean pushEnabledFlag)
    {
        this(sensor, name, pushEnabledFlag, 1, 1.0, 5);
    }
    
    
    public FakeSensorData(FakeSensor sensor, final String name, final boolean pushEnabled, final int bufferSize, final double samplingPeriod, final int maxSampleCount)
    {
        super(sensor);
        this.name = name;
        this.pushEnabled = pushEnabled;
        this.bufferSize = bufferSize;
        this.samplingPeriod = samplingPeriod;
        this.dataQueue = new LinkedBlockingDeque<DataBlock>(bufferSize);
        this.maxSampleCount = maxSampleCount;
        
        if (pushEnabled)
            this.eventHandler = new BasicEventHandler(); 
    }
    
    
    protected void start()
    {
        // start data production timer
        TimerTask sensorTask = new TimerTask()
        {
            @Override
            public void run()
            {
                synchronized (dataQueue)
                {
                    if (Math.random() > 0.8)
                        return;
                    
                    double time = System.currentTimeMillis() / 1000.;
                    DataBlock data = new DataBlockDouble(4);
                    data.setDoubleValue(0, time);
                    data.setDoubleValue(1, 1.0 + Math.random()*0.01);
                    data.setDoubleValue(2, 2.0 + Math.random()*0.01);
                    data.setDoubleValue(3, 3.0 + Math.random()*0.01);
                    
                    count++;                    
                    if (count >= maxSampleCount)
                        cancel();
                    
                    if (dataQueue.size() == bufferSize)
                        dataQueue.remove();
                    dataQueue.offer(data);
                    
                    if (pushEnabled)
                        eventHandler.publishEvent(new SensorDataEvent(time, FakeSensorData.this, data));
                }                        
            }                
        };
        
        Timer timer = new Timer(name, true);
        timer.scheduleAtFixedRate(sensorTask, 0, (long)(samplingPeriod * 1000));
    }
    
    
    @Override
    public boolean isEnabled()
    {
        if (count >= maxSampleCount)
            return false;
        else
            return true;
    }    


    @Override
    public boolean isPushSupported()
    {
        return pushEnabled;
    }
    
    
    @Override
    public boolean isStorageSupported()
    {
        return (bufferSize > 0);
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return samplingPeriod;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        DataComponent record = new DataRecordImpl(3);
        record.setName(this.name);
        record.setDefinition("urn:blabla:weatherData");
        
        Time time = new TimeImpl();
        time.setDefinition("urn:blabla:samplingTime");
        time.getUom().setHref(Time.ISO_TIME_UNIT);
        record.addComponent("time", time);
        
        Quantity temp = new QuantityImpl();
        temp.setDefinition("urn:blabla:temperature");
        temp.getUom().setCode("Cel");
        record.addComponent("temp", temp);
        
        Quantity wind = new QuantityImpl();
        wind.setDefinition("urn:blabla:windSpeed");
        wind.getUom().setCode("m/s");
        record.addComponent("windSpeed", wind);
        
        Quantity press = new QuantityImpl();
        press.setDefinition("urn:blabla:pressure");
        press.getUom().setCode("hPa");
        record.addComponent("press", press);
        
        return record;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return new TextEncodingImpl(",", "\n");
    }


    @Override
    public DataBlock getLatestRecord() throws SensorException
    {
        synchronized (dataQueue)
        {
            return dataQueue.peek();
        }
    }
    
    
    @Override
    public double getLatestRecordTime()
    {
        synchronized (dataQueue)
        {
            return dataQueue.peek().getDoubleValue(0);
        }
    }


    @Override
    public int getStorageCapacity() throws SensorException
    {
        return bufferSize;
    }


    @Override
    public int getNumberOfAvailableRecords() throws SensorException
    {
        synchronized (dataQueue)
        {
            return dataQueue.size();
        }
    }


    @Override
    public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException
    {
        synchronized (dataQueue)
        {
            List<DataBlock> records = new ArrayList<DataBlock>();
            
            if (clear)
            {
                for (int i=0; i<maxRecords; i++)
                    records.add(0, dataQueue.pollLast());
            }
            else
            {
                for (int i=0; i<maxRecords; i++)
                    records.add(dataQueue.peek());
            }
            
            return records;
        }
    }


    @Override
    public List<DataBlock> getAllRecords(boolean clear) throws SensorException
    {
        synchronized (dataQueue)
        {
            List<DataBlock> records = new ArrayList<DataBlock>();
            
            if (clear)
            {
                while (!dataQueue.isEmpty())
                    records.add(0, dataQueue.poll());
            }
            else
            {
                for (DataBlock data: dataQueue)
                    records.add(data);
            }
            
            return records;
        } 
    }


    @Override
    public int clearAllRecords() throws SensorException
    {
        synchronized (dataQueue)
        {
            int numRecords = dataQueue.size();
            dataQueue.clear();
            return numRecords;
        }
    }

}
