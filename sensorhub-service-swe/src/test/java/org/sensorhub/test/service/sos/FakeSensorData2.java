/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sos;

import java.util.List;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorException;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.cdm.common.DataType;
import org.vast.data.DataArray;
import org.vast.data.DataBlockByte;
import org.vast.data.DataGroup;
import org.vast.data.DataValue;
import org.vast.sensorML.system.SMLSystem;
import org.vast.sweCommon.SweConstants;


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
    String name;
    boolean pushEnabled;
    SMLSystem sml;
    int count;
    
    
    public FakeSensorData2(String name, boolean pushEnabled)
    {
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
    public double getAverageSamplingRate()
    {
        return 0.01;
    }


    @Override
    public DataComponent getRecordDescription() throws SensorException
    {
        DataArray img = new DataArray(ARRAY_SIZE);
        img.setProperty(SweConstants.DEF_URI, "urn:blabla:image");
        img.setName(this.name);        
        DataComponent record = new DataGroup(3, this.name);        
        DataValue r = new DataValue(DataType.BYTE);
        record.addComponent("red", r);
        DataValue g = new DataValue(DataType.BYTE);
        record.addComponent("green", g);
        DataValue b = new DataValue(DataType.BYTE);
        record.addComponent("blue", b);        
        img.addComponent(record);        
        return img;
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
