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
import java.util.List;
import java.util.Map;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.ITimeSeriesDataStore;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
public class GenericStreamStorage extends AbstractModule<StreamStorageConfig> implements IBasicStorage<StreamStorageConfig>, IEventListener
{
    private static final Logger log = LoggerFactory.getLogger(GenericStreamStorage.class);
    
    IBasicStorage<StorageConfig> storage;
    WeakReference<IDataProducerModule<?>> dataSourceRef;
    
    
    @SuppressWarnings("unused")
    @Override
    public void init(StreamStorageConfig config) throws SensorHubException
    {
        super.init(config);
        
        // instantiate underlying storage
        StorageConfig storageConfig = config.storageConfig;
        try
        {
            Class<IBasicStorage<StorageConfig>> clazz = (Class<IBasicStorage<StorageConfig>>)Class.forName(storageConfig.moduleClass);
            storage = clazz.newInstance();
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
        // start the underlyign storage
        storage.start();        
        
        IDataProducerModule<?> dataSource = dataSourceRef.get();
        if (dataSource != null)
        {        
            // register to data events
            if (config.selectedOutputs == null || config.selectedOutputs.length == 0)
            {
                for (IStreamingDataInterface output: dataSource.getAllOutputs().values())
                    output.registerListener(this);
            }
            else
            {
                for (String outputName: config.selectedOutputs)
                    dataSource.getAllOutputs().get(outputName).registerListener(this);
            }
            
            // if storage is empty, initialize it
            if (storage.getLatestDataSourceDescription() == null)
                StorageHelper.configureStorageForDataSource(dataSource, storage, false);
            
            // otherwise get the latest sensor description in case we were down during the last update
            else
                storage.storeDataSourceDescription(dataSource.getCurrentDescription());
        }
        else
            log.warn("Data source is unavailable for stream storage " + MsgUtils.moduleString(this));
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
    public void handleEvent(Event e)
    {
        if (isEnabled())
        {
            // new data events
            if (e instanceof DataEvent)
            {
                boolean saveAutoCommitState = storage.isAutoCommit();
                storage.setAutoCommit(false);
                
                // get datastore for output name
                String outputName = ((SensorDataEvent) e).getSource().getName();
                ITimeSeriesDataStore<?> dataStore = storage.getDataStores().get(outputName);
                
                String producer = ((DataEvent) e).getSource().getParentModule().getLocalID();
                
                for (DataBlock record: ((SensorDataEvent) e).getRecords())
                {
                    DataKey key = new DataKey(producer, e.getTimeStamp()/1000.);
                    dataStore.store(key, record);
                    if (log.isTraceEnabled())
                    {
                        log.trace("Storing record " + key.timeStamp + " in DB");
                        log.trace("DB size: " + dataStore.getNumRecords());
                    }
                }
                
                storage.commit();
                storage.setAutoCommit(saveAutoCommitState);
            }
            
            else if (e instanceof SensorEvent)
            {
                if (((SensorEvent) e).getType() == SensorEvent.Type.SENSOR_CHANGED)
                {
                    try
                    {
                        // TODO check that description was actually updated?
                        // in the current state, the same description would be added at each restart
                        // should we compare contents? if not, on what time tag can we rely on?
                        // AbstractSensorModule implementation of getLastSensorDescriptionUpdate() is
                        // only useful between restarts since it will be resetted to current time at startup...
                        
                        // TODO to manage this issue, first check that no other description is valid at the same time
                        storage.storeDataSourceDescription(dataSourceRef.get().getCurrentDescription());
                    }
                    catch (SensorHubException ex)
                    {
                        log.error("Error while updating sensor description", ex);
                    }
                }
            }
        }
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
    public List<AbstractProcess> getDataSourceDescriptionHistory()
    {
        return storage.getDataSourceDescriptionHistory();
    }


    @Override
    public AbstractProcess getDataSourceDescriptionAtTime(double time)
    {
        return storage.getDataSourceDescriptionAtTime(time);
    }


    @Override
    public void storeDataSourceDescription(AbstractProcess process) throws StorageException
    {
        storage.storeDataSourceDescription(process);        
    }


    @Override
    public void updateDataSourceDescription(AbstractProcess process) throws StorageException
    {
        storage.updateDataSourceDescription(process);        
    }


    @Override
    public void removeDataSourceDescription(double time)
    {
        storage.removeDataSourceDescription(time);        
    }


    @Override
    public void removeDataSourceDescriptionHistory()
    {
        storage.removeDataSourceDescriptionHistory();        
    }


    @Override
    public Map<String, ? extends ITimeSeriesDataStore<IDataFilter>> getDataStores()
    {
        return storage.getDataStores();
    }


    @Override
    public ITimeSeriesDataStore<IDataFilter> addNewDataStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding) throws StorageException
    {
        return storage.addNewDataStore(name, recordStructure, recommendedEncoding);
    }
}
