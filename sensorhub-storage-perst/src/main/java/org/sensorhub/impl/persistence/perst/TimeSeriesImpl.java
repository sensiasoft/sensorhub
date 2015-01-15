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

import java.util.Iterator;
import java.util.Map.Entry;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.garret.perst.Index;
import org.garret.perst.IterableIterator;
import org.garret.perst.Key;
import org.garret.perst.Persistent;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.ITimeSeriesDataStore;
import org.sensorhub.api.persistence.StorageDataEvent;
import org.sensorhub.api.persistence.StorageEvent;
import org.sensorhub.impl.common.BasicEventHandler;


/**
 * <p>
 * PERST implementation of an individual time series data store
 * </p>
 *
 * <p>Copyright (c) 2014</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Jan 7, 2015
 */
class TimeSeriesImpl extends Persistent implements ITimeSeriesDataStore<IDataFilter>
{
    Index<DataBlock> recordIndex;
    DataComponent recordDescription;
    DataEncoding recommendedEncoding;
    transient BasicEventHandler eventHandler;
    transient BasicStorageImpl parentStorage;

    
    /*
     * Implementation of an individual time series record
     */
    class DBRecord extends Persistent implements IDataRecord<DataKey>
    {
        DataKey key;
        DataBlock value;
        
        // default constructor needed for PERST on Android JVM
        DBRecord() {}
        
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
    

    // default constructor needed for PERST on Android JVM
    TimeSeriesImpl()
    {
        eventHandler = new BasicEventHandler();
    }


    TimeSeriesImpl(BasicStorageImpl parentStorage, DataComponent recordDescription, DataEncoding recommendedEncoding)
    {
        this();
        this.parentStorage = parentStorage;
        this.recordDescription = recordDescription;
        this.recommendedEncoding = recommendedEncoding;
        recordIndex = parentStorage.db.<DataBlock> createIndex(new Class[] { Double.class, String.class }, true);
    }


    @Override
    public IStorageModule<?> getParentStorage()
    {
        return parentStorage;
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
            keyVals = new Object[] { key.timeStamp };
        else
            keyVals = new Object[] { key.timeStamp, key.producerID };

        return new Key(keyVals);
    }


    protected final Key[] generateKeys(IDataFilter filter)
    {
        Key[] keyRange = new Key[2];
        Object[] keyVals1, keyVals2;

        if (filter.getProducerID() == null)
        {
            keyVals1 = new Object[] { filter.getTimeStampRange()[0] };
            keyVals2 = new Object[] { filter.getTimeStampRange()[1] };
        }
        else
        {
            keyVals1 = new Object[] { filter.getTimeStampRange()[0], filter.getProducerID() };
            keyVals2 = new Object[] { filter.getTimeStampRange()[1], filter.getProducerID() };
        }

        keyRange[0] = new Key(keyVals1);
        keyRange[1] = new Key(keyVals2);
        return keyRange;
    }


    protected final DBRecord generateRecord(Entry<Object, DataBlock> indexEntry)
    {
        Object[] keys = (Object[]) indexEntry.getKey();
        DataKey key = new DataKey((String) keys[1], (double) keys[0]);
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
        return new Iterator<DBRecord>()
        {

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
        if (parentStorage.autoCommit)
            getStorage().commit();
        eventHandler.publishEvent(new StorageDataEvent(System.currentTimeMillis(), this, data));
        return key;
    }


    @Override
    public void update(DataKey key, DataBlock data)
    {
        DataBlock oldData = recordIndex.set(generatePerstKey(key), data);
        getStorage().deallocate(oldData);
        if (parentStorage.autoCommit)
            getStorage().commit();
        eventHandler.publishEvent(new StorageEvent(System.currentTimeMillis(), parentStorage.getLocalID(), StorageEvent.Type.UPDATE));
    }


    @Override
    public void remove(DataKey key)
    {
        DataBlock oldData = recordIndex.remove(generatePerstKey(key));
        getStorage().deallocate(oldData);
        if (parentStorage.autoCommit)
            getStorage().commit();
        eventHandler.publishEvent(new StorageEvent(System.currentTimeMillis(), parentStorage.getLocalID(), StorageEvent.Type.DELETE));
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
            getStorage().deallocate(oldData);
            it.remove();
        }

        if (parentStorage.autoCommit)
            getStorage().commit();

        return count;
    }


    @Override
    public double[] getDataTimeRange()
    {
        IterableIterator<Entry<Object, DataBlock>> it;
        it = recordIndex.entryIterator(BasicStorageImpl.KEY_DATA_START_ALL_TIME, BasicStorageImpl.KEY_DATA_END_ALL_TIME, Index.ASCENT_ORDER);
        if (!it.hasNext())
            return new double[] { 0.0, 0.0 };
        Entry<Object, DataBlock> first = it.next();

        it = recordIndex.entryIterator(BasicStorageImpl.KEY_DATA_START_ALL_TIME, BasicStorageImpl.KEY_DATA_END_ALL_TIME, Index.DESCENT_ORDER);
        Entry<Object, DataBlock> last = it.next();

        Object[] key1 = (Object[]) first.getKey();
        Object[] key2 = (Object[]) last.getKey();
        return new double[] { (double) key1[0], (double) key2[0] };
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