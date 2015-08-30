/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.nexrad;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.DataType;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Time;

import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataRecordImpl;
import org.vast.data.QuantityImpl;
import org.vast.data.SWEFactory;
import org.vast.data.TimeImpl;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;


public class NexradOutput extends AbstractSensorOutput<NexradSensor>
{
	private static final Logger log = LoggerFactory.getLogger(NexradOutput.class);
	DataComponent nexradStruct;
	DataBlock latestRecord;
	BinaryEncoding encoding;
	boolean sendData;
	Timer timer;	
	static int NUM_BINS = 720;  // this should be fixed at construction time as part of the config

	public NexradOutput(NexradSensor parentSensor)
	{
		super(parentSensor);
	}


	@Override
	public String getName()
	{
		return "NexradData";
	}


	@Override
	protected void init()
	{
		//  Add Location only as ouptut- Alex is adding support for this
//		SweHelper.newLocationVectorLLa(...);
		
		// SWE Common data structure
		nexradStruct = new DataRecordImpl(5);
		nexradStruct.setName(getName());
		nexradStruct.setDefinition("http://sensorml.com/ont/swe/property/Location");

		//  Time,el,az,data[]
		Time time = new TimeImpl();
		time.getUom().setHref(Time.ISO_TIME_UNIT);
		time.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
		nexradStruct.addComponent("time", time);

		Quantity el;
		el = new QuantityImpl();
		el.getUom().setCode("deg");
		el.setDefinition("http://sensorml.com/ont/swe/property/ElevationAngle");
		nexradStruct.addComponent("elevation",el);

		Quantity az = new QuantityImpl();
		az.getUom().setCode("deg");
		az.setDefinition("http://sensorml.com/ont/swe/property/AzimuthAngle");
		nexradStruct.addComponent("azimuth",az);

		SWEFactory fac = new SWEFactory();
		Count numBins = fac.newCount(DataType.INT);
		numBins.setDefinition("http://sensorml.com/ont/swe/property/NumberOfSamples"); 
//		numBins.setId("NUM_BINS");
		numBins.setValue(NUM_BINS);  // this needs to be variable but not working as configured below
		nexradStruct.addComponent("count",numBins);

		DataRecord productRecord = new DataRecordImpl(3);

		Quantity reflQuant = fac.newQuantity(DataType.FLOAT);
		reflQuant.setDefinition("http://sensorml.com/ont/swe/propertyx/Reflectivity");  // does not exist- will be reflectivity,velocity,or spectrumWidth- choice here?
		reflQuant.getUom().setCode("db");
		DataArray reflData = fac.newDataArray();
		reflData.setElementType("Reflectivity", reflQuant);
		reflData.setElementCount(numBins); //  alex adding support for this
		productRecord.addComponent("Reflectivity", reflData);

		Quantity velQuant = fac.newQuantity(DataType.FLOAT);
		velQuant.setDefinition("http://sensorml.com/ont/swe/propertyx/Velocity");  // does not exist- will be reflectivity,velocity,or spectrumWidth- choice here?
		velQuant.getUom().setCode("m/s");
		DataArray velData = fac.newDataArray();
		velData.setElementType("Velocity", velQuant);
		velData.setElementCount(numBins); 
		productRecord.addComponent("Velocity", velData);

		Quantity swQuant = fac.newQuantity(DataType.FLOAT);
		swQuant.setDefinition("http://sensorml.com/ont/swe/propertyx/SpectrumWidth");  // does not exist- will be reflectivity,velocity,or spectrumWidth- choice here?
		swQuant.getUom().setCode("1"); // ? or db
		DataArray swData = fac.newDataArray();
		swData.setElementType("SpectrumWidth", swQuant);
		swData.setElementCount(numBins); 
		productRecord.addComponent("SpectrumWidth", swData);

		nexradStruct.addComponent("data", productRecord);

		encoding = SWEHelper.getDefaultBinaryEncoding(nexradStruct);
	}


	private void sendRadial() throws IOException
	{
		//  What will really be happening. We will be getting one full sweep every 5 to 6 minutes, and then a pause
		//  So need to sim this somehow
		String testFile = "C:/Data/sensorhub/Level2/HTX/KHTX20110427_205716_V03";
		Level2Reader reader = new Level2Reader();
		Sweep sweep = reader.readSweep(testFile, Level2Product.REFLECTIVITY);

		// build and publish datablock
		DataBlock dataBlock = nexradStruct.createDataBlock();
		Radial first = sweep.getRadials().get(0);
		long time = (long)first.radialStartTime / 1000;
		dataBlock.setLongValue(0, time);
		dataBlock.setDoubleValue(1, first.elevation);
		dataBlock.setDoubleValue(2, first.azimuth);
		dataBlock.setIntValue(first.numGates);
		dataBlock.setUnderlyingObject(first.dataFloat);

		//        latestRecord = dataBlock;
		eventHandler.publishEvent(new SensorDataEvent(1, NexradOutput.this, dataBlock));
	}


	protected void start()
	{
		if (timer != null)
			return;
		timer = new Timer();

		// start main measurement generation thread
		TimerTask task = new TimerTask() {
			public void run()
			{
				try {
					sendRadial();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}            
		};

		timer.scheduleAtFixedRate(task, 0, (long)(getAverageSamplingPeriod()*1000));        
	}


	protected void stop()
	{
		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}
	}


	@Override
	public double getAverageSamplingPeriod()
	{
		return 1.0;
	}


	@Override
	public DataComponent getRecordDescription()
	{
		return nexradStruct;
	}


	@Override
	public DataEncoding getRecommendedEncoding()
	{
		return encoding;
	}


	@Override
	public DataBlock getLatestRecord()
	{
		return latestRecord;
	}


	@Override
	public long getLatestRecordTime()
	{
		if (latestRecord != null)
			return latestRecord.getLongValue(0);

		return 0;
	}

}
