/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.processing;

import java.io.File;
import java.io.IOException;
import net.opengis.swe.v20.DataComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.processing.DataSourceConfig.InputLinkConfig;
import org.sensorhub.api.processing.IStreamProcess;
import org.sensorhub.api.processing.StreamProcessConfig;
import org.sensorhub.api.processing.StreamingDataSourceConfig;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.SensorHubConfig;
import org.sensorhub.test.sensor.FakeSensor;
import org.sensorhub.test.sensor.FakeSensorData;
import org.vast.cdm.common.DataStreamWriter;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.AsciiDataWriter;
import org.vast.swe.SWECommonUtils;


public class TestStreamProcess implements IEventListener
{
    static String FAKE_SENSOR1_ID = "FAKE_SENSOR1";
    static String NAME_OUTPUT1 = "weather";
    static final double SAMPLING_PERIOD = 0.1;
    static final int SAMPLE_COUNT = 10;
    final static String auto = StreamingDataSourceConfig.AUTO_CREATE;
    
    File configFile;
    DataStreamWriter writer;
    int sampleCount = 0;
        
    
    @Before
    public void setupFramework() throws Exception
    {
        // init sensorhub
        configFile = new File("junit-test.json");
        configFile.deleteOnExit();
        SensorHub.createInstance(new SensorHubConfig(configFile.getAbsolutePath(), configFile.getParent()));
    }
    
    
    protected ISensorModule<?> createSensorDataSource1() throws Exception
    {
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.enabled = false;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.id = FAKE_SENSOR1_ID;
        sensorCfg.name = "Sensor1";
        IModule<?> sensor = SensorHub.getInstance().getModuleRegistry().loadModule(sensorCfg);
        FakeSensorData sensorOutput = new FakeSensorData((FakeSensor)sensor, NAME_OUTPUT1, 10, SAMPLING_PERIOD, SAMPLE_COUNT);
        ((FakeSensor)sensor).setDataInterfaces(sensorOutput);
        sensorOutput.registerListener(this);
        
        return (FakeSensor)sensor;
    }
    
    
    protected StreamingDataSourceConfig buildDataSourceConfig(IModule<?> srcModule, String[] srcPaths, String[] destPaths) throws Exception
    {
        // create process data source config
        StreamingDataSourceConfig dataSrcCfg = new StreamingDataSourceConfig();
        dataSrcCfg.producerID = srcModule.getLocalID();
        
        for (int i = 0; i < srcPaths.length; i++)
        {
            InputLinkConfig inputLink = new InputLinkConfig();
            inputLink.source = NAME_OUTPUT1 + srcPaths[i];
            inputLink.destination = destPaths[i];
            dataSrcCfg.inputConnections.add(inputLink);
        }
        
        return dataSrcCfg;
    }
    
    
    protected IStreamProcess<?> createStreamProcess(Class<?> processClass, StreamingDataSourceConfig... dataSources) throws Exception
    {
        StreamProcessConfig processCfg = new StreamProcessConfig();
        processCfg.enabled = false;
        processCfg.name = "Processing #1";
        processCfg.moduleClass = processClass.getCanonicalName();
        for (StreamingDataSourceConfig dataSrc: dataSources)
            processCfg.dataSources.add(dataSrc);
        
        IStreamProcess<?> process = (IStreamProcess<?>)SensorHub.getInstance().getModuleRegistry().loadModule(processCfg);
        for (IStreamingDataInterface output: process.getAllOutputs().values())
            output.registerListener(this);
        
        return process;
    }
    
    
    protected void testDummyProcess(IStreamProcess<?> process) throws Exception
    {
        SWECommonUtils sweUtils = new SWECommonUtils();
        
        // print out process inputs/outputs/parameters
        System.out.println("** Inputs **");
        for (DataComponent input: process.getInputDescriptors().values())
        {
            System.out.println(input.getName() + ":");
            sweUtils.writeComponent(System.out, input, false, true);
        }
        System.out.println("\n** Outputs **");
        for (DataComponent output: process.getOutputDescriptors().values())
        {
            System.out.println(output.getName() + ":");
            sweUtils.writeComponent(System.out, output, false, true);
        }
        System.out.println("\n** Params **");
        for (DataComponent param: process.getParameters().values())
        {
            System.out.println(param.getName() + ":");
            sweUtils.writeComponent(System.out, param, false, true);
        }
        
        // prepare event writer
        writer = new AsciiDataWriter();
        writer.setDataEncoding(new TextEncodingImpl(",", ""));
        writer.setOutput(System.out);
        
        process.start();
        SensorHub.getInstance().getModuleRegistry().getModuleById(FAKE_SENSOR1_ID).start();
        
        synchronized (this) 
        {
            while (sampleCount < SAMPLE_COUNT)
                wait();
        }
        
        System.out.println();
    }
    
    
    @Test
    public void testDummyProcessAutoIOAll() throws Exception
    {
        ISensorModule<?> sensor1 = createSensorDataSource1();
        IStreamProcess<?> process = createStreamProcess(DummyProcessAutoIO.class, buildDataSourceConfig(
                sensor1,
                new String[] {"/"},
                new String[] {auto}));
        testDummyProcess(process);
    }
    
    
    @Test
    public void testDummyProcessAutoIOOneField() throws Exception
    {
        ISensorModule<?> sensor1 = createSensorDataSource1();
        IStreamProcess<?> process = createStreamProcess(DummyProcessAutoIO.class, buildDataSourceConfig(
                sensor1,
                new String[] {"/windSpeed"},
                new String[] {auto}));
        testDummyProcess(process);
    }
    
    
    @Test
    public void testDummyProcessAutoIOTwoFields() throws Exception
    {
        ISensorModule<?> sensor1 = createSensorDataSource1();
        IStreamProcess<?> process = createStreamProcess(DummyProcessAutoIO.class, buildDataSourceConfig(
                sensor1,
                new String[] {"/windSpeed", "/temp"},
                new String[] {auto, auto}));
        testDummyProcess(process);
    }
    
    
    @Test
    public void testDummyProcessFixedIO() throws Exception
    {
        ISensorModule<?> sensor1 = createSensorDataSource1();
        IStreamProcess<?> process = createStreamProcess(DummyProcessFixedIO.class, buildDataSourceConfig(
                sensor1,
                new String[] {"/press"},
                new String[] {DummyProcessFixedIO.INPUT_NAME}));
        testDummyProcess(process);
    }
    
    
    @Override
    public void handleEvent(Event e)
    {
        if (e instanceof DataEvent)
        {
            try
            {
                System.out.print(((DataEvent)e).getSource().getName() + ": ");
                writer.setDataComponents(((DataEvent)e).getRecordDescription());
                writer.reset();                    
                writer.write(((DataEvent)e).getRecords()[0]);
                writer.flush();
                System.out.println();
                
                if (e.getSource() instanceof DummyOutput)
                    sampleCount++;
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
            
            synchronized (this) { this.notify(); }
        }
    }
    
        
    @After
    public void cleanup()
    {
        try
        {
            if (configFile != null)
                configFile.delete();
            SensorHub.getInstance().stop();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
