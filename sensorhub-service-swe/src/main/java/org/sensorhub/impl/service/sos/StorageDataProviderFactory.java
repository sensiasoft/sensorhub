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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.FoiFilter;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IRecordStorageModule;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.utils.MsgUtils;
import org.vast.data.DataIterator;
import org.vast.ogc.om.IObservation;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.ows.sos.SOSDataFilter;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.ows.swe.SWESOfferingCapabilities;
import org.vast.swe.SWEConstants;
import org.vast.util.Bbox;
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
    final IRecordStorageModule<?> storage;
    SOSOfferingCapabilities caps;
    
    
    protected StorageDataProviderFactory(StorageDataProviderConfig config) throws SensorHubException
    {
        this.config = config;
        IStorageModule<?> storageModule = null;
        
        // get handle to data storage instance
        try
        {
            storageModule = SensorHub.getInstance().getPersistenceManager().getModuleById(config.storageID);
            this.storage = (IRecordStorageModule<?>)storageModule;
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
                caps.setIdentifier("urn:offering:" + storage.getLocalID());
            
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
            getObservablePropertiesFromStorage(caps.getObservableProperties());
            
            // add phenomenon time = period of data available in storage
            caps.setPhenomenonTime(getTimeExtentFromStorage());
        
            // add procedure ID
            caps.getProcedures().add(storage.getLatestDataSourceDescription().getUniqueIdentifier());
            
            // supported formats
            caps.getResponseFormats().add(SWESOfferingCapabilities.FORMAT_OM2);
            caps.getProcedureFormats().add(SWESOfferingCapabilities.FORMAT_SML2);
            
            // FOI stuff
            if (storage instanceof IObsStorage)
            {
                // FOI IDs
                getFoisFromStorage(caps.getRelatedFeatures());
                
                // observed area = bounding rectangle of all FOIs
                getFoisSpatialExtentFromStorage();
            }
            
            // obs types
            getObservationTypesFromStorage(caps.getObservationTypes());
            
            return caps;
        }
        catch (Exception e)
        {
            throw new ServiceException("Error while generating capabilities for sensor " + MsgUtils.moduleString(storage), e);
        }
    }
    
    
    @Override
    public void updateCapabilities() throws ServiceException
    {
        try
        {
            // update time extent
            TimeExtent newTimeExtent = getTimeExtentFromStorage();
            caps.setPhenomenonTime(newTimeExtent);
            
            // update FOI list and BBOX
            if (storage instanceof IObsStorage)
            {
                getFoisFromStorage(caps.getRelatedFeatures());
                getFoisSpatialExtentFromStorage();
            }
        }
        catch (Exception e)
        {
            throw new ServiceException("Error while updating capabilities for sensor " + MsgUtils.moduleString(storage), e);
        }
    }


    /*
     * Gets time extents for all records from storage 
     */
    protected TimeExtent getTimeExtentFromStorage()
    {
        TimeExtent timeExtent = new TimeExtent();
        
        // process outputs descriptions
        for (Entry<String, ? extends IRecordStoreInfo> entry: storage.getRecordStores().entrySet())
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
     * Builds list of FOI IDs from storage
     */
    protected void getFoisFromStorage(Set<String> foiIDs)
    {
        FoiFilter filter = new FoiFilter();
        int numFois = ((IObsStorage) storage).getNumFois(filter);
        if (numFois < config.maxFois)
        {
            Iterator<String> it = ((IObsStorage)storage).getFoiIDs(filter);
            while (it.hasNext())
                foiIDs.add(it.next());
        }
    }
    
    
    /*
     * Gets FOIs bounding rectangle from storage
     */
    protected void getFoisSpatialExtentFromStorage()
    {
        Bbox bbox = ((IObsStorage) storage).getFoisSpatialExtent();
        if (bbox != null)
        {
            if (caps.getObservedAreas().size() == 0)
                caps.getObservedAreas().add(bbox);
            else
                caps.getObservedAreas().set(0, bbox);
        }
    }
    
    
    /*
     * Builds list of observable properties by scanning record structure of each data store
     */
    protected void getObservablePropertiesFromStorage(Set<String> observables)
    {
        // process outputs descriptions
        for (Entry<String, ? extends IRecordStoreInfo> entry: storage.getRecordStores().entrySet())
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
                    observables.add(defUri);
            }
        }
    }
    
    
    /*
     * Build list of observation types by scanning record structure of each data store
     */
    protected void getObservationTypesFromStorage(Set<String> obsTypes)
    {
        obsTypes.add(IObservation.OBS_TYPE_GENERIC);
        obsTypes.add(IObservation.OBS_TYPE_SCALAR);
        
        // process outputs descriptions
        for (Entry<String, ? extends IRecordStoreInfo> entry: storage.getRecordStores().entrySet())
        {
            // skip hidden outputs
            if (config.hiddenOutputs != null && config.hiddenOutputs.contains(entry.getKey()))
                continue;
            
            // obs type depends on top-level component
            DataComponent recordStruct = entry.getValue().getRecordDescription();
            if (recordStruct instanceof DataRecord)
                obsTypes.add(IObservation.OBS_TYPE_RECORD);
            else if (recordStruct instanceof DataArray)
                obsTypes.add(IObservation.OBS_TYPE_ARRAY);
        }
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
    public ISOSDataProvider getNewDataProvider(SOSDataFilter filter) throws ServiceException
    {
        checkEnabled();
        return new StorageDataProvider(storage, config, filter);
    }    
    
    
    @Override
    public Iterator<AbstractFeature> getFoiIterator(final IFoiFilter filter) throws Exception
    {
        if (storage instanceof IObsStorage)
            return ((IObsStorage) storage).getFois(filter);
        
        return Collections.EMPTY_LIST.iterator();
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
