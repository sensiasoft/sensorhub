/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.service.sos;

import java.util.LinkedHashMap;
import java.util.Map;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorInterface;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorException;
import org.vast.sensorML.system.SMLSystem;
import org.vast.util.DateTime;


public class FakeSensor implements ISensorInterface<SensorConfig>
{
    SensorConfig config;
    Map<String, ISensorDataInterface> outputs;
    
    
    public FakeSensor()
    {        
    }
    
    
    public void setDataInterfaces(ISensorDataInterface... outputs) throws SensorException
    {
        this.outputs = new LinkedHashMap<String, ISensorDataInterface>();
        for (ISensorDataInterface o: outputs)
            this.outputs.put(o.getRecordDescription().getName(), o);
    }
    
    
    @Override
    public boolean isEnabled()
    {
        return config.enabled;
    }
    
    
    @Override
    public void init(SensorConfig config) throws SensorHubException
    {
        this.config = config;        
    }


    @Override
    public void updateConfig(SensorConfig config) throws SensorHubException
    {
    }


    @Override
    public SensorConfig getConfiguration()
    {
        return config;
    }


    @Override
    public String getName()
    {
        return config.name;
    }


    @Override
    public String getLocalID()
    {
        return config.id;
    }


    @Override
    public void cleanup() throws SensorHubException
    {
    }


    @Override
    public void saveState(IModuleStateSaver saver) throws SensorHubException
    {
    }


    @Override
    public void loadState(IModuleStateLoader loader) throws SensorHubException
    {
    }


    @Override
    public boolean isSensorDescriptionUpdateSupported()
    {
        return false;
    }


    @Override
    public boolean isSensorDescriptionHistorySupported()
    {
        return false;
    }


    @Override
    public SMLSystem getCurrentSensorDescription() throws SensorException
    {
        SMLSystem sml = new SMLSystem();
        sml.setIdentifier("urn:sensors:mysensor:001");        
        return sml;
    }


    @Override
    public SMLSystem getSensorDescription(DateTime t) throws SensorException
    {
        return getCurrentSensorDescription();
    }


    @Override
    public void updateSensorDescription(SMLSystem systemDesc, boolean recordHistory) throws SensorException
    {
    }


    @Override
    public Map<String, ISensorDataInterface> getAllOutputs() throws SensorException
    {
        return outputs;
    }


    @Override
    public Map<String, ISensorDataInterface> getStatusOutputs() throws SensorException
    {
        return null;
    }


    @Override
    public Map<String, ISensorDataInterface> getObservationOutputs() throws SensorException
    {
        return outputs;
    }


    @Override
    public Map<String, ISensorControlInterface> getCommandInputs() throws SensorException
    {
        return null;
    }


    @Override
    public boolean isConnected()
    {
        return true;
    }


    @Override
    public void stop()
    {
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
