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
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IRecordInfo;
import org.sensorhub.api.persistence.StorageException;
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
 * Factory for storage data providers.
 * </p>
 * <p>
 * This factory is associated to an SOS offering and is persistent
 * throughout the lifetime of the service, so it must be threadsafe.
 * </p>
 * <p>
 * However, the server will obtain a new data provider instance from this
 * factory for each incoming request so the providers themselves don't need
 * to be threadsafe. 
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 15, 2013
 */
public class StorageDataProviderFactory implements IDataProviderFactory
{
    final StorageDataProviderConfig config;
    final IBasicStorage<?> storage;
    SOSOfferingCapabilities caps;
    
    
    protected StorageDataProviderFactory(StorageDataProviderConfig config) throws SensorHubException
    {
        this.config = config;
        IModule<?> storageModule = null;
        
        // get handle to data storage instance
        try
        {
            storageModule = SensorHub.getInstance().getPersistenceManager().getModuleById(config.storageID);
            this.storage = (IBasicStorage<?>)storageModule;
        }
        catch (ClassCastException e)
        {
            throw new ServiceException("Storage " + MsgUtils.moduleString(storageModule) + " is not a supported data storage", e);
        }
    }
    
    
    @Override
    public SOSOfferingCapabilities generateCapabilities() throws ServiceException
    {
        checkEnabled();
        
        try
        {
            caps = new SOSOfferingCapabilities();
            
            // identifier
            if (config.uri != null)
                caps.setIdentifier(config.uri);
            else
                caps.setIdentifier("baseURL#" + storage.getLocalID()); // TODO obtain baseURL
            
            // name
            if (config.name != null)
                caps.setTitle(config.name);
            else
                caps.setTitle(storage.getName());
            
            // description
            if (config.description != null)
                caps.setDescription(config.description);
            else
                caps.setDescription("Data available from storage " + storage.getName());
            
            // observable properties
            Set<String> outputDefs = getObservablePropertiesFromStorage();
            caps.getObservableProperties().addAll(outputDefs);
            
            // observed area ??
            
            // add phenomenon time = period of data available in storage
            caps.setPhenomenonTime(getTimeExtentFromStorage());
        
            // add procedure ID
            caps.getProcedures().add(storage.getLatestDataSourceDescription().getUniqueIdentifier());
            
            // supported formats
            caps.getResponseFormats().add(SWESOfferingCapabilities.FORMAT_OM2);
            caps.getProcedureFormats().add(SWESOfferingCapabilities.FORMAT_SML2);
            
            // TODO foi types (when using an obs storage)
            
            // obs types
            Set<String> obsTypes = getObservationTypesFromStorage();
            caps.getObservationTypes().addAll(obsTypes);
            
            return caps;
        }
        catch (SensorHubException e)
        {
            throw new ServiceException("Error while generating capabilities for sensor " + MsgUtils.moduleString(storage), e);
        }
    }
    
    
    @Override
    public void updateCapabilities() throws ServiceException
    {
        try
        {
            TimeExtent newTimeExtent = getTimeExtentFromStorage();
            caps.setPhenomenonTime(newTimeExtent);
        }
        catch (StorageException e)
        {
            throw new ServiceException("Error while updating capabilities for sensor " + MsgUtils.moduleString(storage), e);
        }        
    }


    /*
     * Builds list of observable properties by scanning record structure of each data store
     */
    protected TimeExtent getTimeExtentFromStorage() throws StorageException
    {
        TimeExtent timeExtent = new TimeExtent();
        
        // process outputs descriptions
        for (Entry<String, ? extends IRecordInfo> entry: storage.getRecordTypes().entrySet())
        {
            // skip hidden outputs
            if (config.hiddenOutputs != null && config.hiddenOutputs.contains(entry.getKey()))
                continue;
            
            double[] storedPeriod = storage.getRecordsTimeRange(entry.getKey());
            
            if (!Double.isNaN(storedPeriod[0]))
            {
                timeExtent.resizeToContain(storedPeriod[0]);
                timeExtent.resizeToContain(storedPeriod[1]);
            }
        }
        
        return timeExtent;
    }
    
    
    /*
     * Builds list of observable properties by scanning record structure of each data store
     */
    protected Set<String> getObservablePropertiesFromStorage() throws StorageException
    {
        HashSet<String> observableUris = new LinkedHashSet<String>();
        
        // process outputs descriptions
        for (Entry<String, ? extends IRecordInfo> entry: storage.getRecordTypes().entrySet())
        {
            // skip hidden outputs
            if (config.hiddenOutputs != null && config.hiddenOutputs.contains(entry.getKey()))
                continue;
            
            // iterate through all SWE components and add all definition URIs as observables
            // this way only composites with URI will get added
            DataComponent recordStruct = entry.getValue().getRecordDescription();
            DataIterator it = new DataIterator(recordStruct);
            while (it.hasNext())
            {
                String defUri = (String)it.next().getDefinition();
                if (defUri != null && !defUri.equals(SWEConstants.DEF_SAMPLING_TIME))
                    observableUris.add(defUri);
            }
        }
        
        return observableUris;
    }
    
    
    /*
     * Build list of observertion types by scanning record structure of each data store
     */
    protected Set<String> getObservationTypesFromStorage() throws StorageException
    {
        HashSet<String> obsTypes = new HashSet<String>();
        obsTypes.add(IObservation.OBS_TYPE_GENERIC);
        
        // process outputs descriptions
        for (Entry<String, ? extends IRecordInfo> entry: storage.getRecordTypes().entrySet())
        {
            // skip hidden outputs
            if (config.hiddenOutputs != null && config.hiddenOutputs.contains(entry.getKey()))
                continue;
            
            // obs type depends on top-level component
            DataComponent recordStruct = entry.getValue().getRecordDescription();
            if (recordStruct instanceof SimpleComponent)
                obsTypes.add(IObservation.OBS_TYPE_SCALAR);
            else if (recordStruct instanceof DataRecord)
                obsTypes.add(IObservation.OBS_TYPE_RECORD);
            else if (recordStruct instanceof DataArray)
                obsTypes.add(IObservation.OBS_TYPE_ARRAY);
        }
        
        return obsTypes;
    }
    
    
    @Override
    public AbstractProcess generateSensorMLDescription(double time)
    {
        if (Double.isNaN(time))
            return storage.getLatestDataSourceDescription();
        else
            return storage.getDataSourceDescriptionAtTime(time);
    }
    
    
    @Override
    public ISOSDataProvider getNewProvider(SOSDataFilter filter) throws ServiceException
    {
        checkEnabled();
        return new StorageDataProvider(storage, config, filter);
    }    
    
    
    /**
     * Checks if provider and underlying sensor are enabled
     * @throws SensorException
     */
    protected void checkEnabled() throws ServiceException
    {
        if (!config.enabled)
        {
            String providerName = (config.name != null) ? config.name : "for " + MsgUtils.moduleString(storage);
            throw new ServiceException("Provider " + providerName + " is disabled");
        }
        
        if (!storage.isEnabled())
            throw new ServiceException("Storage " + MsgUtils.moduleString(storage) + " is disabled");
    }


    @Override
    public boolean isEnabled()
    {
        return (config.enabled && storage.isEnabled());
    }


    @Override
    public void cleanup()
    {

    }

}
