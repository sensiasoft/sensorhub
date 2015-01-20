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

package org.sensorhub.impl.service.sos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.SimpleComponent;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.utils.MsgUtils;
import org.vast.data.DataIterator;
import org.vast.ogc.om.IObservation;
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.ows.swe.SWESOfferingCapabilities;
import org.vast.swe.SWEConstants;
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
    final SensorDataProviderConfig config;
    final ISensorModule<?> sensor;
        
    
    protected SensorDataProviderFactory(SensorDataProviderConfig config) throws SensorHubException
    {
        this.config = config;
        
        // get handle to sensor instance using sensor manager
        this.sensor = SensorHub.getInstance().getSensorManager().getModuleById(config.sensorID);
        
        // register to module lifecycle events
        //SensorHub.getInstance().registerListener(this);
    }
    
    
    @Override
    public SOSOfferingCapabilities generateCapabilities() throws ServiceException
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
                caps.setDescription("Data produced by " + sensor.getName());
            
            // observable properties
            List<String> sensorOutputDefs = getObservablePropertiesFromSensor();
            caps.getObservableProperties().addAll(sensorOutputDefs);
            
            // observed area ??
            
            // phenomenon time
            TimeExtent phenTime = new TimeExtent();
            phenTime.setBaseAtNow(true);
            phenTime.setTimeStep(getLowestSamplingPeriodFromSensor());
            caps.setPhenomenonTime(phenTime);
        
            // use sensor uniqueID as procedure ID
            caps.getProcedures().add(sensor.getCurrentSensorDescription().getUniqueIdentifier());
            
            // supported formats
            caps.getResponseFormats().add(SWESOfferingCapabilities.FORMAT_OM2);
            caps.getProcedureFormats().add(SWESOfferingCapabilities.FORMAT_SML2);
            
            // TODO foi types
            
            // obs types
            List<String> obsTypes = getObservationTypesFromSensor();
            caps.getObservationTypes().addAll(obsTypes);
            
            return caps;
        }
        catch (SensorException e)
        {
            throw new ServiceException("Error while generating capabilities for sensor " + MsgUtils.moduleString(sensor), e);
        }
    }
    
    
    @Override
    public void updateCapabilities() throws Exception
    {
        
    }


    protected List<String> getObservablePropertiesFromSensor() throws SensorException
    {
        List<String> observableUris = new ArrayList<String>();
        
        // process outputs descriptions
        for (Entry<String, ? extends ISensorDataInterface> entry: sensor.getAllOutputs().entrySet())
        {
            // skip hidden outputs
            if (config.hiddenOutputs != null && config.hiddenOutputs.contains(entry.getKey()))
                continue;
            
            // iterate through all SWE components and add all definition URIs as observables
            // this way only composites with URI will get added
            ISensorDataInterface output = entry.getValue();
            DataIterator it = new DataIterator(output.getRecordDescription());
            while (it.hasNext())
            {
                String defUri = (String)it.next().getDefinition();
                if (defUri != null && !defUri.equals(SWEConstants.DEF_SAMPLING_TIME))
                    observableUris.add(defUri);
            }
        }
        
        return observableUris;
    }
    
    
    protected List<String> getObservationTypesFromSensor() throws SensorException
    {
        List<String> obsTypes = new ArrayList<String>();
        obsTypes.add(IObservation.OBS_TYPE_GENERIC);
        
        // process outputs descriptions
        for (Entry<String, ? extends ISensorDataInterface> entry: sensor.getAllOutputs().entrySet())
        {
            // skip hidden outputs
            if (config.hiddenOutputs != null && config.hiddenOutputs.contains(entry.getKey()))
                continue;
            
            // obs type depends on top-level component
            ISensorDataInterface output = entry.getValue();
            DataComponent dataStruct = output.getRecordDescription();
            if (dataStruct instanceof SimpleComponent)
                obsTypes.add(IObservation.OBS_TYPE_SCALAR);
            else if (dataStruct instanceof DataRecord)
                obsTypes.add(IObservation.OBS_TYPE_RECORD);
            else if (dataStruct instanceof DataArray)
                obsTypes.add(IObservation.OBS_TYPE_ARRAY);
        }
        
        return obsTypes;
    }
    
    
    protected double getLowestSamplingPeriodFromSensor() throws SensorException
    {
        double lowestSamplingPeriod = Double.POSITIVE_INFINITY;
        
        // process outputs descriptions
        for (Entry<String, ? extends ISensorDataInterface> entry: sensor.getAllOutputs().entrySet())
        {
            // skip hidden outputs
            if (config.hiddenOutputs != null && config.hiddenOutputs.contains(entry.getKey()))
                continue;
            
            double samplingPeriod = entry.getValue().getAverageSamplingPeriod();
            if (samplingPeriod < lowestSamplingPeriod)
                lowestSamplingPeriod = samplingPeriod;
        }
        
        return lowestSamplingPeriod;
    }
    
    
    @Override
    public AbstractProcess generateSensorMLDescription(double time) throws ServiceException
    {
        checkEnabled();
        
        try
        {
            if (Double.isNaN(time))
                return sensor.getCurrentSensorDescription();
            else
                return sensor.getSensorDescription(time);
        }
        catch (SensorException e)
        {
            throw new ServiceException("Cannot retrieve SensorML description of sensor " + MsgUtils.moduleString(sensor), e);
        }
    }

    
    @Override
    public ISOSDataProvider getNewProvider(SOSDataFilter filter) throws ServiceException
    {
        checkEnabled();
        return new SensorDataProvider(sensor, filter);
    }
    
    
    /*
     * Checks if provider and underlying sensor are enabled
     */
    protected void checkEnabled() throws ServiceException
    {
        if (!config.enabled)
        {
            String providerName = (config.name != null) ? config.name : "for " + config.sensorID;
            throw new ServiceException("Provider " + providerName + " is disabled");
        }
        
        if (!sensor.isEnabled())
            throw new ServiceException("Sensor " + MsgUtils.moduleString(sensor) + " is disabled");
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
        //SensorHub.getInstance().unregisterListener(this);
    }


    @Override
    public boolean isEnabled()
    {
        return (config.enabled && sensor.isEnabled());
    }
}
