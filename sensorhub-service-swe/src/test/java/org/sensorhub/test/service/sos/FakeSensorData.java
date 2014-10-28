/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sos;

import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.vast.cdm.common.AsciiEncoding;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.cdm.common.DataEncoding;
import org.vast.cdm.common.DataType;
import org.vast.data.DataBlockDouble;
import org.vast.data.DataGroup;
import org.vast.data.DataValue;
import org.vast.sensorML.system.SMLSystem;
import org.vast.sweCommon.SweConstants;


/**
 * <p>
 * Fake sensor implementation for testing SOS service
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 20, 2013
 */
public class FakeSensorData implements ISensorDataInterface
{
    static int MAX_COUNT = 5;
    
    FakeSensor sensor;
    String name;
    boolean pushEnabled;
    SMLSystem sml;
    int count;
    int bufferSize;
    double samplingPeriod; // seconds
    Deque<DataBlock> dataQueue;
    BasicEventHandler eventHandler;
    
    
    public FakeSensorData(FakeSensor sensor, String name, boolean pushEnabledFlag)
    {
        this(sensor, name, pushEnabledFlag, 1, 1.0);
    }
    
    
    public FakeSensorData(FakeSensor sensor, final String name, final boolean pushEnabled, final int bufferSize, final double samplingPeriod)
    {
        this.sensor = sensor;
        this.name = name;
        this.pushEnabled = pushEnabled;
        this.bufferSize = bufferSize;
        this.samplingPeriod = samplingPeriod;
        this.dataQueue = new LinkedBlockingDeque<DataBlock>(bufferSize);
        
        /*DataComponent dataDesc = null;
        if (pushEnabled)
        {
            this.eventHandler = new BasicEventHandler();        
            
            try
            {
                dataDesc = getRecordDescription();
            }
            catch (SensorException e)
            {
                throw new RuntimeException("Cannot create FakeSensor", e);
            }
        }*/
        
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
                    
                    DataBlock data = new DataBlockDouble(4);
                    data.setDoubleValue(0, new Date().getTime()/1000.0);
                    data.setDoubleValue(1, 1.0 + Math.random()*0.01);
                    data.setDoubleValue(2, 2.0 + Math.random()*0.01);
                    data.setDoubleValue(3, 3.0 + Math.random()*0.01);
                    
                    count++;                    
                    if (count >= MAX_COUNT)
                        cancel();
                    
                    if (dataQueue.size() == bufferSize)
                        dataQueue.remove();
                    dataQueue.offer(data);
                    
                    //if (pushEnabled)
                    //    eventHandler.publishEvent(new SensorDataEvent(null, dataDesc, data));
                }                        
            }                
        };
        
        Timer timer = new Timer(name, true);
        timer.scheduleAtFixedRate(sensorTask, 0, (long)(samplingPeriod * 1000));
    }
    
    
    @Override
    public boolean isEnabled()
    {
        if (count >= MAX_COUNT)
            return false;
        else
            return true;
    }    
    
    
    @Override
    public ISensorModule<?> getSensorInterface()
    {
        return sensor;
    }


    @Override
    public boolean isStorageSupported()
    {
        return false;
    }


    @Override
    public boolean isPushSupported()
    {
        return pushEnabled;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return samplingPeriod;
    }


    @Override
    public DataComponent getRecordDescription() throws SensorException
    {
        DataComponent record = new DataGroup(3, this.name);
        record.setProperty(SweConstants.DEF_URI, "urn:blabla:weatherData");
        
        DataValue time = new DataValue(DataType.DOUBLE);
        time.setProperty(SweConstants.DEF_URI, SweConstants.DEF_SAMPLING_TIME);
        time.setProperty(SweConstants.UOM_URI, SweConstants.ISO_TIME_DEF);
        record.addComponent("time", time);
        
        DataValue temp = new DataValue(DataType.DOUBLE);
        temp.setProperty(SweConstants.DEF_URI, "urn:blabla:temperature");
        temp.setProperty(SweConstants.UOM_CODE, "Cel");
        record.addComponent("temp", temp);
        
        DataValue wind = new DataValue(DataType.DOUBLE);
        wind.setProperty(SweConstants.DEF_URI, "urn:blabla:windSpeed");
        wind.setProperty(SweConstants.UOM_CODE, "m/s");
        record.addComponent("windSpeed", wind);
        
        DataValue press = new DataValue(DataType.DOUBLE);
        press.setProperty(SweConstants.DEF_URI, "urn:blabla:pressure");
        press.setProperty(SweConstants.UOM_CODE, "hPa");
        record.addComponent("press", press);
        
        return record;
    }


    @Override
    public DataEncoding getRecommendedEncoding() throws SensorException
    {
        return new AsciiEncoding("\n", ",");
    }


    @Override
    public DataBlock getLatestRecord() throws SensorException
    {
        synchronized (dataQueue)
        {
            return dataQueue.poll();
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
