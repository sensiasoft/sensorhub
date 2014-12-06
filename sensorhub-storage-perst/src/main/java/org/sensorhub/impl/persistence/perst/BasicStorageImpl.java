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

package org.sensorhub.impl.persistence.perst;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.opengis.gml.v32.AbstractTimeGeometricPrimitive;
import net.opengis.gml.v32.TimeInstant;
import net.opengis.gml.v32.TimePeriod;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.garret.perst.Index;
import org.garret.perst.IterableIterator;
import org.garret.perst.Key;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;
import org.garret.perst.StorageFactory;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.ITimeSeriesDataStore;
import org.sensorhub.api.persistence.StorageDataEvent;
import org.sensorhub.api.persistence.StorageEvent;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * Basic implementation of a PERST based persistent storage of data records.
 * This class must be listed in the META-INF services folder to be available via the persistence manager.
 * </p>
 *
 * <p>Copyright (c) 2014</p>
 * @author Alexandre Robin
 * @since Nov 15, 2014
 */
public class BasicStorageImpl extends AbstractModule<BasicStorageConfig> implements IBasicStorage<BasicStorageConfig>
{
    private static Key KEY_START_ALL_TIME = new Key(Double.NEGATIVE_INFINITY);
    private static Key KEY_END_ALL_TIME = new Key(Double.POSITIVE_INFINITY);
    
    protected Storage db;
    protected DBRoot dbRoot;
    protected boolean autoCommit;
    
    
    /*
     * Default constructor necessary for java service loader
     */
    public BasicStorageImpl()
    {
        this.eventHandler = new BasicEventHandler();
    }
    
    
    @Override
    public void start() throws StorageException
    {
        try
        {
            this.autoCommit = true;
            
            // first make sure it's not already opened
            if (db != null && db.isOpened())
                throw new StorageException("Storage " + getLocalID() + " is already opened");
            
            db = StorageFactory.getInstance().createStorage();            
            db.open(config.storagePath, config.memoryCacheSize*1024);
            dbRoot = (DBRoot)db.getRoot();
            
            if (dbRoot == null)
            { 
                dbRoot = new DBRoot();                
                db.setRoot(dbRoot);
            }
        }
        catch (Exception e)
        {
            throw new StorageException("Error while opening storage " + config.name, e);
        }
    }


    @Override
    public void stop() throws SensorHubException
    {
        db.close();
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        // remove database file?
    }
    
    
    @Override
    public void backup(OutputStream os) throws IOException
    {
        db.backup(os);   
    }


    @Override
    public void restore(InputStream is) throws IOException
    {
        
        
    }


    @Override
    public void setAutoCommit(boolean autoCommit)
    {
        this.autoCommit = autoCommit;        
    }


    @Override
    public boolean isAutoCommit()
    {
        return autoCommit;
    }


    @Override
    public final void commit()
    {
        db.commit();
    }


    @Override
    public void rollback()
    {
        db.rollback();        
    }


    @Override
    public void sync(IStorageModule<?> storage)
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public AbstractProcess getLatestDataSourceDescription()
    {
        Iterator<AbstractProcess> it = dbRoot.descriptionTimeIndex.iterator(new Key(-Double.MAX_VALUE), new Key(Double.MAX_VALUE), Index.DESCENT_ORDER);
        if (it.hasNext())
            return it.next();
        return null;
    }


    @Override
    public List<AbstractProcess> getDataSourceDescriptionHistory()
    {
        return Collections.unmodifiableList(dbRoot.descriptionTimeIndex.getList(KEY_START_ALL_TIME, KEY_END_ALL_TIME));
    }


    @Override
    public AbstractProcess getDataSourceDescriptionAtTime(double time)
    {
        Iterator<AbstractProcess> it = dbRoot.descriptionTimeIndex.iterator(KEY_START_ALL_TIME, new Key(time), Index.DESCENT_ORDER);
        if (it.hasNext())
            return it.next();
        return null;
    }


    @Override
    public void storeDataSourceDescription(AbstractProcess process) throws StorageException
    {
        // we add the description in index for each validity period/instant
        for (AbstractTimeGeometricPrimitive validTime: process.getValidTimeList())
        {
            double time = Double.NaN;
            
            try
            {
                if (validTime instanceof TimeInstant)
                    time = ((TimeInstant) validTime).getTimePosition().getDateTimeValue().getAsDouble();
                else if (validTime instanceof TimePeriod)
                    time = ((TimePeriod) validTime).getBeginPosition().getDateTimeValue().getAsDouble();
            }
            catch (Exception e)
            {
                throw new StorageException("Sensor description must contain at least one validity period");
            }
            
            if (!Double.isNaN(time))
                dbRoot.descriptionTimeIndex.put(new Key(time), process);
        }
        
        if (autoCommit)
            commit();
    }


    @Override
    public void updateDataSourceDescription(AbstractProcess process) throws StorageException
    {
        // TODO Auto-generated method stub
        
        //db.deallocate(oldObject);
        if (autoCommit)
            commit();
    }


    @Override
    public void removeDataSourceDescription(double time)
    {
        Iterator<AbstractProcess> it = dbRoot.descriptionTimeIndex.iterator(KEY_START_ALL_TIME, new Key(time), Index.DESCENT_ORDER);
        if (it.hasNext())
        {
            AbstractProcess sml = it.next();
            it.remove();
            db.deallocate(sml);
        }
        
        if (autoCommit)
            commit();
    }


    @Override
    public void removeDataSourceDescriptionHistory()
    {
        Iterator<AbstractProcess> it = dbRoot.descriptionTimeIndex.iterator(KEY_START_ALL_TIME, KEY_END_ALL_TIME, Index.ASCENT_ORDER);
        while (it.hasNext())
        {
            AbstractProcess sml = it.next();
            it.remove();
            db.deallocate(sml);
        }
        
        if (autoCommit)
            commit();
    }


    @Override
    public Map<String, ? extends ITimeSeriesDataStore<IDataFilter>> getDataStores()
    {
        return Collections.unmodifiableMap(dbRoot.dataStores);
    }


    @Override
    public ITimeSeriesDataStore<IDataFilter> addNewDataStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding) throws StorageException
    {
        TimeSeriesImpl newTimeSeries = new TimeSeriesImpl(recordStructure, recommendedEncoding);
        dbRoot.dataStores.put(name, newTimeSeries);
        if (autoCommit)
            commit();
        return newTimeSeries;
    }

    
    /*
     * Root of storage
     */
    private class DBRoot extends Persistent
    {
        Index<AbstractProcess> descriptionTimeIndex;
        Map<String, TimeSeriesImpl> dataStores;
        
        public DBRoot()
        {
            dataStores = db.<String,TimeSeriesImpl>createMap(String.class, 10);
            descriptionTimeIndex = db.<AbstractProcess>createIndex(double.class, true);
        }
    }
    
    
    /*
     * Implementation of an individual time series record
     */
    private class DBRecord extends Persistent implements IDataRecord<DataKey>
    {
        DataKey key;
        DataBlock value;
        
        protected DBRecord(DataKey key, DataBlock value)
        {
            this.key = key;
            this.value = value;
        }
        
        @Override
        public DataKey getKey()
        {
            return this.key;
        }

        @Override
        public DataBlock getData()
        {
            return this.value;
        }     
    }
    
    
    /*
     * Implementation of an individual time series data store
     */
    private class TimeSeriesImpl extends Persistent implements ITimeSeriesDataStore<IDataFilter>
    {
        Index<DataBlock> recordIndex;
        DataComponent recordDescription;
        DataEncoding recommendedEncoding;
        transient BasicEventHandler eventHandler;
        
        TimeSeriesImpl(DataComponent recordDescription, DataEncoding recommendedEncoding)
        {
            this.recordDescription = recordDescription;
            this.recommendedEncoding = recommendedEncoding;
            this.eventHandler = new BasicEventHandler();
            recordIndex = db.<DataBlock>createIndex(new Class[] {Double.class, String.class}, true);
        }
        
        @Override
        public IStorageModule<?> getParentStorage()
        {
            return BasicStorageImpl.this;
        }

        @Override
        public int getNumRecords()
        {
            return recordIndex.size();
        }        
        
        @Override
        public DataComponent getRecordDescription()
        {
            return recordDescription;
        }
        
        @Override
        public DataEncoding getRecommendedEncoding()
        {
            return recommendedEncoding;
        }
        
        protected final Key generatePerstKey(DataKey key)
        {
            Object[] keyVals;            
            if (key.producerID == null)
                keyVals = new Object[] {key.timeStamp};
            else
                keyVals = new Object[] {key.timeStamp, key.producerID};
            
            return new Key(keyVals);
        }
        
        protected final Key[] generateKeys(IDataFilter filter)
        {
            Key[] keyRange = new Key[2];
            Object[] keyVals1, keyVals2;
            
            if (filter.getProducerID() == null)
            {
                keyVals1 = new Object[] {filter.getTimeStampRange()[0]};
                keyVals2 = new Object[] {filter.getTimeStampRange()[1]};
            }
            else
            {
                keyVals1 = new Object[] {filter.getTimeStampRange()[0], filter.getProducerID()};
                keyVals2 = new Object[] {filter.getTimeStampRange()[1], filter.getProducerID()};                
            }
            
            keyRange[0] = new Key(keyVals1);
            keyRange[1] = new Key(keyVals2);
            return keyRange;
        }
        
        protected final DBRecord generateRecord(Entry<Object, DataBlock> indexEntry)
        {
            Object[] keys = (Object[])indexEntry.getKey();
            DataKey key = new DataKey((String)keys[1], (double)keys[0]);
            return new DBRecord(key, indexEntry.getValue());
        }
        
        @Override
        public DataBlock getDataBlock(DataKey key)
        {
            return recordIndex.get(generatePerstKey(key));
        }

        @Override
        public IterableIterator<DataBlock> getDataBlockIterator(IDataFilter filter)
        {
            Key[] keyRange = generateKeys(filter);            
            return recordIndex.iterator(keyRange[0], keyRange[1], Index.ASCENT_ORDER);
        }

        @Override
        public IDataRecord<DataKey> getRecord(DataKey key)
        {
            Key perstKey = generatePerstKey(key);
            IterableIterator<Entry<Object, DataBlock>> it = recordIndex.entryIterator(perstKey, perstKey, Index.ASCENT_ORDER);
            if (it.hasNext())
                return generateRecord(it.next());
            else
                return null;
        }

        @Override
        public int getNumMatchingRecords(IDataFilter filter)
        {
            IterableIterator<DataBlock> it = getDataBlockIterator(filter);
            return it.size();
        }
        
        @Override
        public Iterator<DBRecord> getRecordIterator(IDataFilter filter)
        {
            Key[] keyRange = generateKeys(filter);            
            final IterableIterator<Entry<Object, DataBlock>> it = recordIndex.entryIterator(keyRange[0], keyRange[1], Index.ASCENT_ORDER);
            return new Iterator<DBRecord>() {

                public final boolean hasNext()
                {
                    return it.hasNext();
                }

                public final DBRecord next()
                {
                    Entry<Object, DataBlock> entry = it.next();
                    return generateRecord(entry);
                }

                public final void remove()
                {
                    it.remove();                    
                }                
            };
        }

        @Override
        public DataKey store(DataKey key, DataBlock data)
        {
            recordIndex.put(generatePerstKey(key), data);
            if (autoCommit)
                commit();                
            eventHandler.publishEvent(new StorageDataEvent(System.currentTimeMillis(), this, data));
            return key;
        }

        @Override
        public void update(DataKey key, DataBlock data)
        {
            DataBlock oldData = recordIndex.set(generatePerstKey(key), data);
            db.deallocate(oldData);
            if (autoCommit)
                commit();
            eventHandler.publishEvent(new StorageEvent(System.currentTimeMillis(), getLocalID(), StorageEvent.Type.UPDATE));
        }

        @Override
        public void remove(DataKey key)
        {
            DataBlock oldData = recordIndex.remove(generatePerstKey(key));
            db.deallocate(oldData);
            if (autoCommit)
                commit();
            eventHandler.publishEvent(new StorageEvent(System.currentTimeMillis(), getLocalID(), StorageEvent.Type.DELETE));
        }

        @Override
        public int remove(IDataFilter filter)
        {
            Key[] keyRange = generateKeys(filter);            
            Iterator<DataBlock> it = recordIndex.iterator(keyRange[0], keyRange[1], Index.ASCENT_ORDER);
            
            int count = 0;
            while (it.hasNext())
            {
                DataBlock oldData = it.next();
                db.deallocate(oldData);
                it.remove();
            }
            
            if (autoCommit)
                commit();
            
            return count;
        }
        
        @Override
        public double[] getDataTimeRange()
        {
            IterableIterator<Entry<Object, DataBlock>> it;
            it = recordIndex.entryIterator(KEY_START_ALL_TIME, KEY_END_ALL_TIME, Index.ASCENT_ORDER);
            if (!it.hasNext())
                return new double[] {0.0, 0.0};
            Entry<Object, DataBlock> first = it.next();
            
            it = recordIndex.entryIterator(KEY_START_ALL_TIME, KEY_END_ALL_TIME, Index.DESCENT_ORDER);
            Entry<Object, DataBlock> last = it.next();
            
            return new double[] {((DataKey)first.getKey()).timeStamp, ((DataKey)last.getKey()).timeStamp};
        }

        @Override
        public void registerListener(IEventListener listener)
        {
            eventHandler.registerListener(listener);
        }

        @Override
        public void unregisterListener(IEventListener listener)
        {
            eventHandler.unregisterListener(listener);
        }
    }

}
