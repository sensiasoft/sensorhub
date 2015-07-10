/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.FoiEvent;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IFoiFilter;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IRecordStorageModule;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.IRecordInfo;
import org.sensorhub.api.persistence.ObsKey;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;
import org.vast.swe.ScalarIndexer;
import org.vast.util.Bbox;


/**
 * <p>
 * Generic wrapper/adapter enabling any storage implementation to store data
 * coming from data events (e.g. sensor data, processed data, etc.)<br/>
 * This class takes care of registering with the appropriate producers and
 * uses the storage API to store records in the underlying storage.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 21, 2015
 */
public class GenericStreamStorage extends AbstractModule<StreamStorageConfig> implements IRecordStorageModule<StreamStorageConfig>, IObsStorage, IEventListener
{
    private static final Logger log = LoggerFactory.getLogger(GenericStreamStorage.class);
    
    IRecordStorageModule<StorageConfig> storage;
    WeakReference<IDataProducerModule<?>> dataSourceRef;
    Map<String, ScalarIndexer> timeStampIndexers = new HashMap<String, ScalarIndexer>();
    Map<String, String> currentFoiMap = new HashMap<String, String>();
    String currentFoi;
    
    
    @Override
    public void init(StreamStorageConfig config) throws SensorHubException
    {
        super.init(config);
        
        // instantiate underlying storage
        StorageConfig storageConfig = null;
        try
        {
            storageConfig = (StorageConfig)config.storageConfig.clone();
            storageConfig.id = getLocalID();
            storageConfig.name = getName();
            Class<?> clazz = (Class<?>)Class.forName(storageConfig.moduleClass);
            storage = (IRecordStorageModule<StorageConfig>)clazz.newInstance();
            storage.init(storageConfig);
        }
        catch (Exception e)
        {
            if (storageConfig == null)
                throw new StorageException("Underlying storage configuration must be provided for generic storage " + config.name);
            else
                throw new StorageException("Cannot instantiate underlying storage " + storageConfig.moduleClass);
        }
        
        // retrieve reference to data source
        ModuleRegistry moduleReg = SensorHub.getInstance().getModuleRegistry();
        dataSourceRef = (WeakReference<IDataProducerModule<?>>)moduleReg.getModuleRef(config.dataSourceID);
    }
    
    
    @Override
    public void start() throws SensorHubException
    {
        // start the underlying storage
        storage.start();
        
        IDataProducerModule<?> dataSource = dataSourceRef.get();
        if (dataSource != null)
        {        
            // if storage is empty, initialize it
            if (storage.getLatestDataSourceDescription() == null)
                configureStorageForDataSource(dataSource, storage);
            
            // otherwise just get the latest sensor description in case we were down during the last update
            else if (dataSource.getLastDescriptionUpdate() != Double.NEGATIVE_INFINITY)
                storage.storeDataSourceDescription(dataSource.getCurrentDescription());
            
            // also init current FOI
            if (dataSource instanceof IMultiSourceDataProducer)
            {
                for (String entityID: ((IMultiSourceDataProducer)dataSource).getEntityIDs())
                {
                    AbstractFeature foi = ((IMultiSourceDataProducer)dataSource).getCurrentFeatureOfInterest(entityID);
                    if (foi != null)
                        currentFoiMap.put(entityID, foi.getUniqueIdentifier());
                }
            }
            else
            {
                AbstractFeature foi = dataSource.getCurrentFeatureOfInterest();
                if (foi != null)
                    currentFoi = foi.getUniqueIdentifier();
            }
            
            // register to data events
            if (config.selectedOutputs == null || config.selectedOutputs.length == 0)
            {
                for (IStreamingDataInterface output: dataSource.getAllOutputs().values())
                    prepareToReceiveEvents(output);
            }
            else
            {
                for (String outputName: config.selectedOutputs)
                    prepareToReceiveEvents(dataSource.getAllOutputs().get(outputName));
            }
        }
        else
            log.warn("Data source is unavailable for stream storage " + MsgUtils.moduleString(this));
    }
    
    
    protected void configureStorageForDataSource(IDataProducerModule<?> dataSource, IRecordStorageModule<?> storage) throws StorageException
    {
        if (storage.getRecordTypes().size() > 0)
            throw new RuntimeException("Storage " + MsgUtils.moduleString(storage) + " is already configured");
        
        // copy sensor description (only current or full history if supported)
        if (dataSource instanceof ISensorModule<?> && ((ISensorModule<?>)dataSource).isSensorDescriptionHistorySupported())
        {
            ISensorModule<?> sensor = ((ISensorModule<?>)dataSource);
            for (AbstractProcess sensorDesc: sensor.getSensorDescriptionHistory())
                storage.storeDataSourceDescription(sensorDesc);
        }
        else
            storage.storeDataSourceDescription(dataSource.getCurrentDescription());
            
        // for multi-source producers, prepare data stores for all entities
        if (dataSource instanceof IMultiSourceDataProducer && storage instanceof IMultiSourceStorage)
        {
            for (String entityID: ((IMultiSourceDataProducer)dataSource).getEntityIDs())
                addProducerInfo(entityID);
        }
        else
        {
            // copy current feature of interest
            if (storage instanceof IObsStorage)
            {
                String producerID = dataSource.getCurrentDescription().getUniqueIdentifier();
                AbstractFeature foi = dataSource.getCurrentFeatureOfInterest();
                if (foi != null)
                    ((IObsStorage)storage).storeFoi(producerID, foi);
            }
            
            // create one data store for each sensor output
            for (Entry<String, ? extends IStreamingDataInterface> item: dataSource.getAllOutputs().entrySet())
            {
                String name = item.getKey();
                IStreamingDataInterface output = item.getValue();
                storage.addRecordType(name, output.getRecordDescription(), output.getRecommendedEncoding());
            }
        }
    }
    
    
    protected void addProducerInfo(String producerID)
    {
        if (storage instanceof IMultiSourceStorage)
        {
            if (((IMultiSourceStorage<?>)storage).getProducerIDs().contains(producerID))
                return;
        
            IDataProducerModule<?> dataSource = dataSourceRef.get();
            if (dataSource != null && dataSource instanceof IMultiSourceDataProducer)
            {
                // create producer data store
                IBasicStorage dataStore = ((IMultiSourceStorage<IBasicStorage>)storage).addDataStore(producerID);
                
                // save producer SensorML description
                dataStore.storeDataSourceDescription(((IMultiSourceDataProducer) dataSource).getCurrentDescription(producerID));
                
                // save current FOI
                AbstractFeature foi = ((IMultiSourceDataProducer) dataSource).getCurrentFeatureOfInterest(producerID);
                if (foi != null)
                    ((IObsStorage)storage).storeFoi(producerID, foi);
                
                // create one data store for each sensor output
                for (Entry<String, ? extends IStreamingDataInterface> item: dataSource.getAllOutputs().entrySet())
                {
                    String name = item.getKey();
                    IStreamingDataInterface output = item.getValue();
                    dataStore.addRecordType(name, output.getRecordDescription(), output.getRecommendedEncoding());
                }
            }
        }
    }
    
    
    protected void prepareToReceiveEvents(IStreamingDataInterface output)
    {
        // create time stamp indexer
        String outputName = output.getName();
        ScalarIndexer timeStampIndexer = timeStampIndexers.get(outputName);
        if (timeStampIndexer == null)
        {
            timeStampIndexer = SWEHelper.getTimeStampIndexer(output.getRecordDescription());
            timeStampIndexers.put(outputName, timeStampIndexer);
        }
        
        output.registerListener(this);
    }
    
        
    @Override
    public void stop() throws SensorHubException
    {
        IDataProducerModule<?> dataSource = dataSourceRef.get();
        if (dataSource != null)
        {            
            if (config.selectedOutputs == null || config.selectedOutputs.length == 0)
            {
                for (IStreamingDataInterface output: dataSource.getAllOutputs().values())
                    output.unregisterListener(this);
            }
            else
            {
                for (String outputName: config.selectedOutputs)
                    dataSource.getAllOutputs().get(outputName).unregisterListener(this);
            }
        }

        storage.stop();
        dataSourceRef = null;
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        storage.cleanup();
    }
    
    
    @Override
    public void handleEvent(Event<?> e)
    {
        if (isEnabled())
        {
            // new data events
            if (e instanceof DataEvent)
            {
                DataEvent dataEvent = (DataEvent)e;
                boolean saveAutoCommitState = storage.isAutoCommit();
                storage.setAutoCommit(false);
                
                // get indexer for looking up time stamp value
                String outputName = dataEvent.getSource().getName();
                ScalarIndexer timeStampIndexer = timeStampIndexers.get(outputName);
                
                for (DataBlock record: dataEvent.getRecords())
                {
                    // get time stamp
                    double time;
                    if (timeStampIndexer != null)
                        time = timeStampIndexer.getDoubleValue(record);
                    else
                        time = e.getTimeStamp() / 1000.;
                    
                    // get FOI ID
                    String foiID;
                    String entityID = dataEvent.getRelatedEntityID();
                    if (entityID != null)
                    {
                        addProducerInfo(entityID); // to handle new producer
                        foiID = currentFoiMap.get(entityID);
                    }
                    else
                        foiID = currentFoi; 
                    
                    // store record with proper key
                    ObsKey key = new ObsKey(outputName, entityID, foiID, time);                    
                    storage.storeRecord(key, record);
                    
                    if (log.isTraceEnabled())
                        log.trace("Storing record " + key.timeStamp + " for output " + outputName);
                }
                
                storage.commit();
                storage.setAutoCommit(saveAutoCommitState);
            }
            
            else if (e instanceof SensorEvent)
            {
                if (((SensorEvent) e).getType() == SensorEvent.Type.SENSOR_CHANGED)
                {
                    // TODO check that description was actually updated?
                    // in the current state, the same description would be added at each restart
                    // should we compare contents? if not, on what time tag can we rely on?
                    // AbstractSensorModule implementation of getLastSensorDescriptionUpdate() is
                    // only useful between restarts since it will be resetted to current time at startup...
                    
                    // TODO to manage this issue, first check that no other description is valid at the same time
                    storage.storeDataSourceDescription(dataSourceRef.get().getCurrentDescription());
                }
            }
            
            else if (e instanceof FoiEvent && storage instanceof IObsStorage)
            {
                FoiEvent foiEvent = (FoiEvent)e;
                String producerID = ((FoiEvent) e).getRelatedEntityID();
                
                // store feature object if specified
                if (foiEvent.getFoi() != null)
                    ((IObsStorage) storage).storeFoi(producerID, foiEvent.getFoi());
                
                if (producerID != null)
                    currentFoiMap.put(producerID, foiEvent.getFoiID());
                else
                    currentFoi = foiEvent.getFoiID();
            }
        }
    }
    

    @Override
    public void addRecordType(String name, DataComponent recordStructure, DataEncoding recommendedEncoding)
    {
        // register new record type with underlying storage
        if (!storage.getRecordTypes().containsKey(name))
            storage.addRecordType(name, recordStructure, recommendedEncoding);
        
        // prepare to receive events
        IDataProducerModule<?> dataSource = dataSourceRef.get();
        if (dataSource != null)
            prepareToReceiveEvents(dataSource.getAllOutputs().get(name));
    }


    @Override
    public void backup(OutputStream os) throws IOException
    {
        storage.backup(os);        
    }


    @Override
    public void restore(InputStream is) throws IOException
    {
        storage.restore(is);        
    }


    @Override
    public void setAutoCommit(boolean autoCommit)
    {
        storage.setAutoCommit(autoCommit);        
    }


    @Override
    public boolean isAutoCommit()
    {
        return storage.isAutoCommit();
    }


    @Override
    public void commit()
    {
        storage.commit();        
    }


    @Override
    public void rollback()
    {
        storage.rollback();        
    }


    @Override
    public void sync(IStorageModule<?> storage) throws StorageException
    {
        storage.sync(storage);        
    }


    @Override
    public AbstractProcess getLatestDataSourceDescription()
    {
        return storage.getLatestDataSourceDescription();
    }


    @Override
    public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double endTime)
    {
        return storage.getDataSourceDescriptionHistory(startTime, endTime);
    }


    @Override
    public AbstractProcess getDataSourceDescriptionAtTime(double time)
    {
        return storage.getDataSourceDescriptionAtTime(time);
    }


    @Override
    public void storeDataSourceDescription(AbstractProcess process)
    {
        storage.storeDataSourceDescription(process);        
    }


    @Override
    public void updateDataSourceDescription(AbstractProcess process)
    {
        storage.updateDataSourceDescription(process);        
    }


    @Override
    public void removeDataSourceDescription(double time)
    {
        storage.removeDataSourceDescription(time);        
    }


    @Override
    public void removeDataSourceDescriptionHistory(double startTime, double endTime)
    {
        storage.removeDataSourceDescriptionHistory(startTime, endTime);
    }


    @Override
    public Map<String, ? extends IRecordInfo> getRecordTypes()
    {
        return storage.getRecordTypes();
    }


    public DataBlock getDataBlock(DataKey key)
    {
        return storage.getDataBlock(key);
    }


    @Override
    public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
        return storage.getDataBlockIterator(filter);
    }


    @Override
    public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter)
    {
        return storage.getRecordIterator(filter);
    }


    @Override
    public int getNumMatchingRecords(IDataFilter filter)
    {
        return storage.getNumMatchingRecords(filter);
    }

    
    @Override
    public int getNumRecords(String recordType)
    {
        return storage.getNumRecords(recordType);
    }


    @Override
    public double[] getRecordsTimeRange(String recordType)
    {
        return storage.getRecordsTimeRange(recordType);
    }


    @Override
    public void storeRecord(DataKey key, DataBlock data)
    {
        storage.storeRecord(key, data);
    }


    @Override
    public void updateRecord(DataKey key, DataBlock data)
    {
        storage.updateRecord(key, data);
    }


    @Override
    public void removeRecord(DataKey key)
    {
        storage.removeRecord(key);
    }


    @Override
    public int removeRecords(IDataFilter filter)
    {
        return storage.removeRecords(filter);
    }


    @Override
    public int getNumFois(IFoiFilter filter)
    {
        if (storage instanceof IObsStorage)
            return ((IObsStorage) storage).getNumFois(filter);
        
        return 0;
    }
    
    
    @Override
    public Bbox getFoisSpatialExtent()
    {
        if (storage instanceof IObsStorage)
            return ((IObsStorage) storage).getFoisSpatialExtent();
        
        return null;
    }


    @Override
    public Iterator<String> getFoiIDs(IFoiFilter filter)
    {
        if (storage instanceof IObsStorage)
            return ((IObsStorage) storage).getFoiIDs(filter);
        
        return Collections.EMPTY_LIST.iterator();
    }


    @Override
    public Iterator<AbstractFeature> getFois(IFoiFilter filter)
    {
        if (storage instanceof IObsStorage)
            return ((IObsStorage) storage).getFois(filter);
        
        return Collections.EMPTY_LIST.iterator();
    }


    @Override
    public void storeFoi(String producerID, AbstractFeature foi)
    {
        if (storage instanceof IObsStorage)
            storeFoi(producerID, foi);        
    }
}
