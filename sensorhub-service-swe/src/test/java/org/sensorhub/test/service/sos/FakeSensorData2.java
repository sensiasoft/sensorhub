/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sos;

import java.nio.ByteOrder;
import java.util.List;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.vast.data.BinaryComponentImpl;
import org.vast.data.BinaryEncodingImpl;
import org.vast.data.CountImpl;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockByte;
import org.vast.data.DataRecordImpl;


/**
 * <p>
 * Fake array sensor implementation for testing SOS service
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 20, 2013
 */
public class FakeSensorData2 implements ISensorDataInterface
{
    static int MAX_COUNT = 2;
    static int ARRAY_SIZE = 12000;
    
    FakeSensor sensor;
    String name;
    boolean pushEnabled;
    int count;
    
    
    public FakeSensorData2(FakeSensor sensor, String name, boolean pushEnabled)
    {
        this.sensor = sensor;
        this.name = name;
        this.pushEnabled = pushEnabled;
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
        return 0.01;
    }


    @Override
    public DataComponent getRecordDescription() throws SensorException
    {
        DataArray img = new DataArrayImpl(ARRAY_SIZE);
        img.setDefinition("urn:blabla:image");
        img.setName(this.name);        
        DataComponent record = new DataRecordImpl(3);        
        Count r = new CountImpl(DataType.BYTE);
        record.addComponent("red", r);
        Count g = new CountImpl(DataType.BYTE);
        record.addComponent("green", g);
        Count b = new CountImpl(DataType.BYTE);
        record.addComponent("blue", b);        
        img.addComponent("pixel", record);        
        return img;
    }
    
    
    @Override
    public DataEncoding getRecommendedEncoding() throws SensorException
    {
        BinaryEncoding dataEnc = new BinaryEncodingImpl();
        dataEnc.setByteEncoding(ByteEncoding.RAW);
        dataEnc.setByteOrder(ByteOrder.BIG_ENDIAN);
        dataEnc.addMemberAsComponent(new BinaryComponentImpl("pixel/red", DataType.BYTE));
        dataEnc.addMemberAsComponent(new BinaryComponentImpl("pixel/green", DataType.BYTE));
        dataEnc.addMemberAsComponent(new BinaryComponentImpl("pixel/blue", DataType.BYTE));
        return dataEnc;
    }


    @Override
    public DataBlock getLatestRecord() throws SensorException
    {
        if (Math.random() > 0.3)
            return null;
        
        count++;
        DataBlock data = new DataBlockByte(3*ARRAY_SIZE);
        for (int i=0; i<ARRAY_SIZE; i++)
            data.setByteValue(i, (byte)(i%255));
        return data;
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
        return 0;
    }


    @Override
    public void registerListener(IEventListener listener)
    {
    }

    
    @Override
    public void unregisterListener(IEventListener listener)
    {
    }    
}
