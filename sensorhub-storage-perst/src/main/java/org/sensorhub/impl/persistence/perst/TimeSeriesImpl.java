/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Map.Entry;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.garret.perst.Index;
import org.garret.perst.IterableIterator;
import org.garret.perst.Key;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IRecordStoreInfo;


/**
 * <p>
 * PERST implementation of a time series data store for a single record type
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jan 7, 2015
 */
class TimeSeriesImpl extends Persistent implements IRecordStoreInfo
{
    static Key KEY_DATA_START_ALL_TIME = new Key(Double.NEGATIVE_INFINITY);
    static Key KEY_DATA_END_ALL_TIME = new Key(Double.POSITIVE_INFINITY);
    
    DataComponent recordDescription;
    DataEncoding recommendedEncoding;
    Index<DataBlock> recordIndex;
    
    
    /*
     * Implementation of an individual time series record
     */
    class DBRecord extends Persistent implements IDataRecord
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
    

    // default constructor needed on Android JVM
    TimeSeriesImpl() { }


    TimeSeriesImpl(Storage db, DataComponent recordDescription, DataEncoding recommendedEncoding)
    {
        super(db);
        this.recordDescription = recordDescription;
        this.recommendedEncoding = recommendedEncoding;
        recordIndex = db.<DataBlock> createIndex(double.class, true);
    }


    @Override
    public String getName()
    {
        return recordDescription.getName();
    }
    
    
    @Override
    public DataComponent getRecordDescription()
    {
        return recordDescription;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        // HACK to fix broken ByteOrder enum
        // java.nio.ByteOrder is a class with static singletons instead of an enum
        // This causes instances deserialized from storage to be unequal with the constant
        // that we compare with everywhere
        if (recommendedEncoding instanceof BinaryEncoding)
        {
            ByteOrder byteOrder = ((BinaryEncoding) recommendedEncoding).getByteOrder();
            if (byteOrder != null && byteOrder != ByteOrder.BIG_ENDIAN && byteOrder != ByteOrder.LITTLE_ENDIAN)
            {
                if (byteOrder.toString().equals(ByteOrder.LITTLE_ENDIAN.toString()))
                    byteOrder = ByteOrder.LITTLE_ENDIAN;
                else
                    byteOrder = ByteOrder.BIG_ENDIAN;
            }
        }
        
        return recommendedEncoding;
    }

    
    int getNumRecords()
    {
        return recordIndex.size();
    }


    DataBlock getDataBlock(DataKey key)
    {
        return recordIndex.get(new Key(key.timeStamp));
    }


    Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
        final Iterator<Entry<Object, DataBlock>> it = getEntryIterator(filter);
        
        return new Iterator<DataBlock>()
        {
            public final boolean hasNext()
            {
                return it.hasNext();
            }

            public final DataBlock next()
            {
                Entry<Object, DataBlock> entry = it.next();
                return entry.getValue();
            }

            public final void remove()
            {
                it.remove();
            }
        };
    }


    int getNumMatchingRecords(IDataFilter filter, long maxCount)
    {
        // use entry iterator so datablocks are not loaded during scan
        IterableIterator<Entry<Object, DataBlock>> it = getEntryIterator(filter);
        
        int count = 0;
        while (it.hasNext() && count <= maxCount)
        {
            it.next();
            count++;
        }
        
        return count;
    }


    Iterator<DBRecord> getRecordIterator(IDataFilter filter)
    {
        final Iterator<Entry<Object, DataBlock>> it = getEntryIterator(filter);
        
        return new Iterator<DBRecord>()
        {
            public final boolean hasNext()
            {
                return it.hasNext();
            }

            public final DBRecord next()
            {
                Entry<Object, DataBlock> entry = it.next();
                DataKey key = new DataKey(recordDescription.getName(), (double)entry.getKey());
                return new DBRecord(key, entry.getValue());
            }

            public final void remove()
            {
                it.remove();
            }
        };
    }
    
    
    protected IterableIterator<Entry<Object,DataBlock>> getEntryIterator(IDataFilter filter)
    {
        double[] timeRange = filter.getTimeStampRange();
        Key keyFirst = new Key(timeRange == null ? Double.NEGATIVE_INFINITY : timeRange[0]);
        Key keyLast = new Key(timeRange == null ? Double.POSITIVE_INFINITY : timeRange[1]);
        return recordIndex.entryIterator(keyFirst, keyLast, Index.ASCENT_ORDER);
    }


    void store(DataKey key, DataBlock data)
    {
        recordIndex.put(new Key(key.timeStamp), data);
    }


    void update(DataKey key, DataBlock data)
    {
        DataBlock oldData = recordIndex.set(new Key(key.timeStamp), data);
        getStorage().deallocate(oldData);
    }


    void remove(DataKey key)
    {
        DataBlock oldData = recordIndex.remove(new Key(key.timeStamp));
        getStorage().deallocate(oldData);
    }


    int remove(IDataFilter filter)
    {
        int count = 0;
        
        Key keyFirst = new Key(filter.getTimeStampRange()[0]);
        Key keyLast = new Key(filter.getTimeStampRange()[1]);
        Iterator<DataBlock> it = recordIndex.iterator(keyFirst, keyLast, Index.ASCENT_ORDER);
            
        while (it.hasNext())
        {
            DataBlock oldData = it.next();
            it.remove();
            getStorage().deallocate(oldData);
        }

        return count;
    }


    double[] getDataTimeRange()
    {
        IterableIterator<Entry<Object, DataBlock>> it;
        it = recordIndex.entryIterator(KEY_DATA_START_ALL_TIME, KEY_DATA_END_ALL_TIME, Index.ASCENT_ORDER);
        if (!it.hasNext())
            return new double[] { Double.NaN, Double.NaN };
        Entry<Object, DataBlock> first = it.next();

        it = recordIndex.entryIterator(KEY_DATA_START_ALL_TIME, KEY_DATA_END_ALL_TIME, Index.DESCENT_ORDER);
        Entry<Object, DataBlock> last = it.next();

        return new double[] { (double)first.getKey(), (double)last.getKey() };
    }
    
    
    public Iterator<double[]> getRecordsTimeClusters(String recordType)
    {
        final IterableIterator<Entry<Object, DataBlock>> it;
        it = recordIndex.entryIterator(KEY_DATA_START_ALL_TIME, KEY_DATA_END_ALL_TIME, Index.ASCENT_ORDER);
        
        return new Iterator<double[]>()
        {
            double lastTime = Double.NaN;
            
            public boolean hasNext()
            {
                return it.hasNext();
            }

            public double[] next()
            {
                double[] clusterTimeRange = new double[2];
                clusterTimeRange[0] = lastTime;
                
                while (it.hasNext())
                {
                    // PERST doesn't load object from disk until getValue() is called so we're good here
                    double recTime = (double)it.next().getKey();
                    
                    if (Double.isNaN(lastTime))
                    {
                        clusterTimeRange[0] = recTime;
                        lastTime = recTime;
                    }
                    else
                    {
                        double dt = recTime - lastTime;
                        lastTime = recTime;
                        if (dt > 60.0)
                            break;
                    }
                    
                    clusterTimeRange[1] = recTime;
                }
                
                return clusterTimeRange;
            }

            public void remove()
            {               
            }    
        };
    }
}