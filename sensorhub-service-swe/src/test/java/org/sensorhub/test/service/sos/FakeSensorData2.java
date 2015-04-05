/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sos;

import java.nio.ByteOrder;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.test.sensor.FakeSensor;
import org.sensorhub.test.sensor.IFakeSensorOutput;
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
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 20, 2013
 */
public class FakeSensorData2 extends AbstractSensorOutput<FakeSensor> implements IFakeSensorOutput
{
    static int MAX_COUNT = 2;
    static int ARRAY_SIZE = 12000;
    
    String name;
    int count;
    
    
    public FakeSensorData2(FakeSensor sensor, String name)
    {
        super(sensor);
        this.name = name;
    }
    
    
    @Override
    public String getName()
    {
        return name;
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
    public double getAverageSamplingPeriod()
    {
        return 0.01;
    }


    @Override
    public DataComponent getRecordDescription()
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
    public DataEncoding getRecommendedEncoding()
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
    public DataBlock getLatestRecord()
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
    public void init()
    {
        // TODO Auto-generated method stub        
    }


    @Override
    public void stop()
    {
        // TODO Auto-generated method stub
        
    }
}
