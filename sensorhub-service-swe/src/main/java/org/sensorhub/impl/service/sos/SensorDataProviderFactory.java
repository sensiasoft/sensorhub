/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorInterface;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.vast.data.DataIterator;
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.sensorML.SMLFeature;
import org.vast.sweCommon.SweConstants;
import org.vast.util.DateTime;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Factory for sensor data providers.
 * </p>
 * <p>
 * One data provider factory is created for each offering and is persistent
 * throughout the lifetime of the service, so it must be threadsafe.
 * </p>
 * <p>
 * However, the server obtains a new data provider instance from the factory
 * for each incoming request so the providers themselves don't need to be
 * threadsafe. 
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 15, 2013
 */
public class SensorDataProviderFactory implements IDataProviderFactory, IEventListener
{
    SensorDataProviderConfig config;
    ISensorInterface<?> sensor;
        
    
    protected SensorDataProviderFactory(SensorDataProviderConfig config)
    {
        this.config = config;
        
        // get handle to sensor instance using sensor manager
        this.sensor = SensorHub.getInstance().getSensorManager().getModuleById(config.sensorID);
        this.config.enabled = this.sensor.getConfiguration().enabled;
        
        // register to module lifecycle events
        ModuleRegistry.getInstance().registerListener(this);
    }
    
    
    @Override
    public SOSOfferingCapabilities generateCapabilities() throws SensorException
    {
        checkEnabled();        
        
        try
        {
            SOSOfferingCapabilities caps = new SOSOfferingCapabilities();
            
            // identifier
            if (config.uri != null)
                caps.setIdentifier(config.uri);
            else
                caps.setIdentifier("baseURL#" + sensor.getLocalID()); // TODO obtain baseURL
            
            // name
            if (config.name != null)
                caps.setTitle(config.name);
            else
                caps.setTitle(sensor.getName());
            
            // description
            if (config.description != null)
                caps.setDescription(config.description);
            else
                caps.setDescription("Data produced by sensor " + sensor.getName());
            
            // observable properties
            List<String> sensorOutputDefs = getObservablePropertiesFromSensor();
            caps.getObservableProperties().addAll(sensorOutputDefs);
            
            // observed area ??
            
            // phenomenon time
            TimeExtent phenTime = new TimeExtent();
            phenTime.setBaseAtNow(true);
            phenTime.setLagTimeDelta(1.0);//sensor.getObservationOutputs().get(0).getAverageSamplingRate());
            caps.getPhenomenonTimes().add(phenTime);
        
            // use sensor uniqueID as procedure ID
            caps.getProcedures().add(sensor.getCurrentSensorDescription().getUniqueID());
            
            // supported formats
            caps.getResponseFormats().add(SOSOfferingCapabilities.FORMAT_OM2);
            caps.getProcedureFormats().add(SOSOfferingCapabilities.FORMAT_SML2);
            
            return caps;
        }
        catch (SensorException e)
        {
            throw new RuntimeException("Error while generating capabilities for sensor " + sensor.getName() + " (" + sensor.getLocalID() + ")");
        }
    }
    
    
    protected List<String> getObservablePropertiesFromSensor() throws SensorException
    {
        List<String> observableUris = new ArrayList<String>();
        
        // process only selected outputs
        for (String outputName: config.selectedOutputs)
        {
            ISensorDataInterface outputInterface = sensor.getAllOutputs().get(outputName);
            
            // iterate through all components and add all definition URIs as observables
            // this way only composite with URI will get added
            DataIterator it = new DataIterator(outputInterface.getRecordDescription());
            while (it.hasNext())
            {
                String defUri = (String)it.next().getProperty(SweConstants.DEF_URI);
                if (defUri != null)
                    observableUris.add(defUri);
            }
            
            // TODO we should probably filter out some components such as the time stamp alone, etc.
        }
        
        return observableUris;
    }
    
    
    @Override
    public SMLFeature generateSensorMLDescription(DateTime t) throws SensorException
    {
        checkEnabled();
        
        if (t == null)
            return sensor.getCurrentSensorDescription();
        else
            return sensor.getSensorDescription(t);
    }

    
    @Override
    public ISOSDataProvider getNewProvider(SOSDataFilter filter) throws SensorException
    {
        checkEnabled();
        return new SensorDataProvider(sensor, filter);
    }
    
    
    /**
     * Checks if provider and underlying sensor are enabled
     * @throws SensorException
     */
    protected void checkEnabled() throws SensorException
    {
        if (!config.enabled)
        {
            String providerName = (config.name != null) ? config.name : "for " + config.sensorID;
            throw new SensorException("Provider " + providerName + " is disabled");
        }
        
        if (!sensor.getConfiguration().enabled)
        {
            throw new SensorException("Sensor " + config.sensorID + " is disabled");
        }
    }


    @Override
    public void handleEvent(Event e)
    {
        /*// we need to enable/disable this provider when the state of the
        // underlying sensor changes
        if (e instanceof ModuleEvent && e.getSource() == sensor)
        {
            if (((ModuleEvent) e).type == ModuleEvent.Type.DELETED)
                config.enabled = false;
            
            if (((ModuleEvent) e).type == ModuleEvent.Type.ENABLED)
                config.enabled = true;
            
            if (((ModuleEvent) e).type == ModuleEvent.Type.DISABLED)
                config.enabled = false;
        }*/       
    }


    @Override
    public void cleanup()
    {
        ModuleRegistry.getInstance().unregisterListener(this);        
    }


    @Override
    public boolean isEnabled()
    {
        return config.enabled;
    }
}
