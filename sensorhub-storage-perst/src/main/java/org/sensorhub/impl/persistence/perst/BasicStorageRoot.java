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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.opengis.gml.v32.AbstractTimeGeometricPrimitive;
import net.opengis.gml.v32.TimeInstant;
import net.opengis.gml.v32.TimePeriod;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.garret.perst.Index;
import org.garret.perst.Key;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IRecordStoreInfo;


/**
 * <p>
 * PERST implementation of a basic record storage fed by a single producer
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since May 8, 2015
 */
class BasicStorageRoot extends Persistent implements IBasicStorage
{
    private static Key KEY_SML_START_ALL_TIME = new Key(Double.NEGATIVE_INFINITY);
    private static Key KEY_SML_END_ALL_TIME = new Key(Double.POSITIVE_INFINITY);
    
    Index<AbstractProcess> descriptionTimeIndex;
    Map<String, TimeSeriesImpl> dataStores;
    
    
    // default constructor needed on Android JVM
    BasicStorageRoot() {}
    
    
    public BasicStorageRoot(Storage db)
    {
        super(db);
        dataStores = new HashMap<String,TimeSeriesImpl>(10);
        descriptionTimeIndex = db.<AbstractProcess>createIndex(double.class, true);
    }
    
    
    @Override
    public AbstractProcess getLatestDataSourceDescription()
    {
        Iterator<AbstractProcess> it = descriptionTimeIndex.iterator(KEY_SML_START_ALL_TIME, KEY_SML_END_ALL_TIME, Index.DESCENT_ORDER);
        if (it.hasNext())
            return it.next();
        return null;
    }


    @Override
    public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double endTime)
    {
        List<AbstractProcess> processList = descriptionTimeIndex.getList(new Key(startTime), new Key(endTime));
        return Collections.unmodifiableList(processList);
    }


    @Override
    public AbstractProcess getDataSourceDescriptionAtTime(double time)
    {
        Iterator<AbstractProcess> it = descriptionTimeIndex.iterator(KEY_SML_START_ALL_TIME, new Key(time), Index.DESCENT_ORDER);
        if (it.hasNext())
            return it.next();
        return null;
    }


    @Override
    public void storeDataSourceDescription(AbstractProcess process)
    {
        if (process.getNumValidTimes() > 0)
        {
            // we add the description in index for each validity period/instant
            for (AbstractTimeGeometricPrimitive validTime: process.getValidTimeList())
            {
                double time = Double.NaN;
                
                if (validTime instanceof TimeInstant)
                    time = ((TimeInstant) validTime).getTimePosition().getDecimalValue();
                else if (validTime instanceof TimePeriod)
                    time = ((TimePeriod) validTime).getBeginPosition().getDecimalValue();
                
                if (!Double.isNaN(time))
                {
                    AbstractProcess oldProcess = descriptionTimeIndex.set(new Key(time), process);
                    if (oldProcess != null)
                        getStorage().deallocate(oldProcess);
                }
            }
        }
        else
        {
            // if no validity period is specified, we just add with current time
            double time = System.currentTimeMillis() / 1000.;
            descriptionTimeIndex.put(new Key(time), process);
        }
    }


    @Override
    public void updateDataSourceDescription(AbstractProcess process)
    {
        // TODO Auto-generated method stub
        
        //db.deallocate(oldObject);
    }


    @Override
    public void removeDataSourceDescription(double time)
    {
        Iterator<AbstractProcess> it = descriptionTimeIndex.iterator(KEY_SML_START_ALL_TIME, new Key(time), Index.DESCENT_ORDER);
        if (it.hasNext())
        {
            AbstractProcess sml = it.next();
            it.remove();
            getStorage().deallocate(sml);
        }
    }


    @Override
    public void removeDataSourceDescriptionHistory(double startTime, double endTime)
    {
        Storage db = getStorage();
        
        Iterator<AbstractProcess> it = descriptionTimeIndex.iterator(new Key(startTime), new Key(endTime), Index.ASCENT_ORDER);
        while (it.hasNext())
        {
            AbstractProcess sml = it.next();
            
            // get end of validity of process description
            double endValidity = Double.NaN; 
            AbstractTimeGeometricPrimitive validTime = sml.getValidTimeList().get(0);
            if (validTime instanceof TimePeriod)
                endValidity = ((TimePeriod) validTime).getEndPosition().getDecimalValue();
            
            // check that end of validity is also within time range
            // if end of validity is now, endValidity will be NaN
            // if this is the last description returned, don't remove it if end of validity is now
            if (endValidity <= endTime || (Double.isNaN(endValidity) && it.hasNext()))
            {
                it.remove();
                db.deallocate(sml);
            }
        }
    }
    
    
    @Override
    public void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding)
    {
        recordStructure.setName(name);
        TimeSeriesImpl newTimeSeries = new TimeSeriesImpl(getStorage(), recordStructure, recommendedEncoding);
        dataStores.put(name, newTimeSeries);
        modify();
    }
    
    
    @Override
    public Map<String, ? extends IRecordStoreInfo> getRecordStores()
    {
        return Collections.unmodifiableMap(dataStores);
    }
    
    
    protected final TimeSeriesImpl getRecordStore(String recordType)
    {
        TimeSeriesImpl dataStore = dataStores.get(recordType);
        if (dataStore == null)
            throw new IllegalArgumentException("Record type not found in this storage: " + recordType);
        
        // make sure parent is set
        dataStore.parentStore = this;
        return dataStore;            
    }


    @Override
    public int getNumRecords(String recordType)
    {
        return getRecordStore(recordType).getNumRecords();
    }

    
    @Override
    public double[] getRecordsTimeRange(String recordType)
    {
        return getRecordStore(recordType).getDataTimeRange();
    }
    
    
    @Override
    public Iterator<double[]> getRecordsTimeClusters(String recordType)
    {
        return getRecordStore(recordType).getRecordsTimeClusters(recordType);
    }
    
    
    @Override
    public DataBlock getDataBlock(DataKey key)
    {
        return getRecordStore(key.recordType).getDataBlock(key);
    }


    @Override
    public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
        return getRecordStore(filter.getRecordType()).getDataBlockIterator(filter);
    }


    @Override
    public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter)
    {
        return getRecordStore(filter.getRecordType()).getRecordIterator(filter);
    }


    @Override
    public int getNumMatchingRecords(IDataFilter filter, long maxCount)
    {
        return getRecordStore(filter.getRecordType()).getNumMatchingRecords(filter, maxCount);
    }
    

    @Override
    public void storeRecord(DataKey key, DataBlock data)
    {
        getRecordStore(key.recordType).store(key, data);
    }


    @Override
    public void updateRecord(DataKey key, DataBlock data)
    {
        getRecordStore(key.recordType).update(key, data);
    }


    @Override
    public void removeRecord(DataKey key)
    {
        getRecordStore(key.recordType).remove(key);
    }


    @Override
    public int removeRecords(IDataFilter filter)
    {
        return getRecordStore(filter.getRecordType()).remove(filter);
    }
}
