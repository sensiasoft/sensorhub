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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.ByteEncoding;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataType;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.data.FoiEvent;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.sensorhub.test.sensor.IFakeSensorOutput;
import org.vast.data.BinaryComponentImpl;
import org.vast.data.DataBlockByte;
import org.vast.swe.SWEHelper;


/**
 * <p>
 * Fake array sensor implementation for testing SOS service
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 20, 2013
 */
public class FakeSensorData2 extends AbstractSensorOutput<FakeSensorNetWithFoi> implements IFakeSensorOutput
{
    static int ARRAY_SIZE = 12000;
    
    String name;
    DataComponent outputStruct;
    DataEncoding outputEncoding;
    int maxSampleCount;
    int sampleCount;
    double samplingPeriod; // seconds
    Map<Integer, Integer> obsFoiMap;
    Timer timer;
    TimerTask sensorTask;
    boolean started;
    boolean hasListeners;
    
    
    public FakeSensorData2(FakeSensorNetWithFoi sensor, String name, double samplingPeriod, int maxSampleCount)
    {
        this(sensor, name, samplingPeriod, maxSampleCount, null);   
    }
    
    
    public FakeSensorData2(FakeSensorNetWithFoi sensor, String name, double samplingPeriod, int maxSampleCount, Map<Integer, Integer> obsFoiMap)
    {
        super(sensor);
        this.name = name;
        this.samplingPeriod = samplingPeriod;
        this.maxSampleCount = maxSampleCount;
        this.obsFoiMap = obsFoiMap;
        init();
    }
    
    
    public void init()
    {        
        // generate output structure and encoding
        SWEHelper fac = new SWEHelper();
        
        DataArray img = fac.newDataArray(ARRAY_SIZE);
        img.setDefinition("urn:blabla:image");
        img.setName(this.name);        
        DataComponent record = fac.newDataRecord(3);        
        record.addComponent("red", fac.newCount("urn:blabla:RedChannel", "Red Channel", null, DataType.BYTE));
        record.addComponent("green", fac.newCount("urn:blabla:GreenChannel", "Green Channel", null, DataType.BYTE));
        record.addComponent("blue", fac.newCount("urn:blabla:BlueChannel", "Blue Channel", null, DataType.BYTE));       
        img.addComponent("pixel", record);     
        this.outputStruct = img; 
        
        BinaryEncoding dataEnc = fac.newBinaryEncoding(ByteOrder.BIG_ENDIAN, ByteEncoding.RAW);
        dataEnc.addMemberAsComponent(new BinaryComponentImpl("pixel/red", DataType.BYTE));
        dataEnc.addMemberAsComponent(new BinaryComponentImpl("pixel/green", DataType.BYTE));
        dataEnc.addMemberAsComponent(new BinaryComponentImpl("pixel/blue", DataType.BYTE));
        this.outputEncoding = dataEnc;
    }
    
    
    @Override
    public String getName()
    {
        return name;
    }
    
    
    public int getMaxSampleCount()
    {
        return maxSampleCount;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return 0.01;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return outputStruct;
    }
    
    
    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return outputEncoding;
    }


    @Override
    public void start()
    {   
        // we start sending if we have listeners
        if (hasListeners)
            startSending();
        started = true;
    }
    
    
    protected void startSending()
    {
        if (timer == null)
        {
            // start data production timer
            sensorTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    // safety to make sure we don't output more samples than requested
                    // cancel does not seem to be taken into account early enough with high rates
                    if (sampleCount >= maxSampleCount)
                        return;
                    
                    DataBlock data = new DataBlockByte(3*ARRAY_SIZE);
                    for (int i=0; i<ARRAY_SIZE; i++)
                        data.setByteValue(i, (byte)(i%255));
                               
                    sampleCount++;
                    latestRecordTime = System.currentTimeMillis();
                    latestRecord = data;
                    
                    if (obsFoiMap != null)
                    {
                        Integer foiNum = obsFoiMap.get(sampleCount);
                        if (foiNum != null)
                        {
                            String entityID = FakeSensorNetWithFoi.SENSOR_UID_PREFIX + foiNum;
                            AbstractFeature foi = FakeSensorData2.this.getParentModule().getCurrentFeatureOfInterest(entityID);
                            eventHandler.publishEvent(new FoiEvent(latestRecordTime, getParentModule(), foi, latestRecordTime/1000.));
                            System.out.println("Observing FOI #" + foiNum);
                        }
                    }
                    
                    eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, FakeSensorData2.this, latestRecord)); 
                    System.out.println("Record #" + sampleCount + " generated");
                    
                    if (sampleCount >= maxSampleCount)
                        cancel();
                }                
            };
                
            timer = new Timer(name, true);
            timer.scheduleAtFixedRate(sensorTask, 0, (long)(samplingPeriod * 1000));
        }
    }


    @Override
    public void stop()
    {
        if (timer != null)
        {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }


    @Override
    public boolean isEnabled()
    {
        if (sampleCount >= maxSampleCount)
            return false;
        else
            return true;
    } 
    
    
    @Override
    public void registerListener(IEventListener listener)
    {
        super.registerListener(listener);
        
        // we start sending only if start has been called
        if (started)
            startSending();
        
        hasListeners = true;
    }
}
