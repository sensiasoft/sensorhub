/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.SimpleComponent;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.utils.MsgUtils;
import org.vast.data.DataIterator;
import org.vast.ogc.om.IObservation;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.ows.swe.SWESOfferingCapabilities;
import org.vast.swe.SWEConstants;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Abstract factory for streaming data providers.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ProducerType> Type of producer handled by this provider
 * @since Feb 28, 2015
 */
public abstract class StreamDataProviderFactory<ProducerType extends IDataProducerModule<?>> implements IDataProviderFactory, IEventListener
{
    final StreamDataProviderConfig config;
    final String producerType;
    final ProducerType producer;    
   
    
    protected StreamDataProviderFactory(StreamDataProviderConfig config, ProducerType producer, String producerType) throws SensorHubException
    {
        this.config = config;
        this.producer = producer;
        this.producerType = producerType;
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
                caps.setIdentifier("baseURL#" + producer.getLocalID()); // TODO obtain baseURL
            
            // name
            if (config.name != null)
                caps.setTitle(config.name);
            else
                caps.setTitle(producer.getName());
            
            // description
            if (config.description != null)
                caps.setDescription(config.description);
            else
                caps.setDescription("Data produced by " + producer.getName());
            
            // observable properties
            Set<String> sensorOutputDefs = getObservablePropertiesFromProducer();
            caps.getObservableProperties().addAll(sensorOutputDefs);
            
            // observed area ??
            
            // phenomenon time
            TimeExtent phenTime = new TimeExtent();
            phenTime.setBaseAtNow(true);
            phenTime.setTimeStep(getLowestSamplingPeriodFromProducer());
            caps.setPhenomenonTime(phenTime);
        
            // use producer uniqueID as procedure ID
            caps.getProcedures().add(producer.getCurrentDescription().getUniqueIdentifier());
            
            // supported formats
            caps.getResponseFormats().add(SWESOfferingCapabilities.FORMAT_OM2);
            caps.getProcedureFormats().add(SWESOfferingCapabilities.FORMAT_SML2);
            
            // TODO foi types
            
            // obs types
            Set<String> obsTypes = getObservationTypesFromProducer();
            caps.getObservationTypes().addAll(obsTypes);
            
            return caps;
        }
        catch (SensorHubException e)
        {
            throw new ServiceException("Error while generating capabilities for " + MsgUtils.moduleString(producer), e);
        }
    }
    
    
    @Override
    public void updateCapabilities() throws Exception
    {
        
    }


    protected Set<String> getObservablePropertiesFromProducer() throws SensorHubException
    {
        HashSet<String> observableUris = new LinkedHashSet<String>();
        
        // scan outputs descriptions
        for (Entry<String, ? extends IStreamingDataInterface> entry: producer.getAllOutputs().entrySet())
        {
            // skip hidden outputs
            if (config.hiddenOutputs != null && config.hiddenOutputs.contains(entry.getKey()))
                continue;
            
            // iterate through all SWE components and add all definition URIs as observables
            // this way only composites with URI will get added
            IStreamingDataInterface output = entry.getValue();
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
    
    
    protected Set<String> getObservationTypesFromProducer() throws SensorHubException
    {
        HashSet<String> obsTypes = new HashSet<String>();
        obsTypes.add(IObservation.OBS_TYPE_GENERIC);
        
        // process outputs descriptions
        for (Entry<String, ? extends IStreamingDataInterface> entry: producer.getAllOutputs().entrySet())
        {
            // skip hidden outputs
            if (config.hiddenOutputs != null && config.hiddenOutputs.contains(entry.getKey()))
                continue;
            
            // obs type depends on top-level component
            IStreamingDataInterface output = entry.getValue();
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
    
    
    protected double getLowestSamplingPeriodFromProducer() throws SensorHubException
    {
        double lowestSamplingPeriod = Double.POSITIVE_INFINITY;
        
        // process outputs descriptions
        for (Entry<String, ? extends IStreamingDataInterface> entry: producer.getAllOutputs().entrySet())
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
            return producer.getCurrentDescription();
        }
        catch (SensorHubException e)
        {
            throw new ServiceException("Cannot retrieve SensorML description of " + MsgUtils.moduleString(producer), e);
        }
    }
    
    
    /*
     * Checks if provider and underlying sensor are enabled
     */
    protected void checkEnabled() throws ServiceException
    {
        if (!config.enabled)
        {
            String providerName = (config.name != null) ? config.name : "for " + config.uri;
            throw new ServiceException(producerType + " " + providerName + " is disabled");
        }
        
        if (!producer.isEnabled())
            throw new ServiceException(producerType + " " + MsgUtils.moduleString(producer) + " is disabled");
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
        return (config.enabled && producer.isEnabled());
    }
}
