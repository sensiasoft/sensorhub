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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.garret.perst.Index;
import org.garret.perst.Key;
import org.garret.perst.Storage;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.ObsKey;
import org.sensorhub.impl.persistence.perst.FoiTimesStoreImpl.FoiTimePeriod;


/**
 * <p>
 * PERST implementation of an observation series data store for a single
 * record type
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since May 8, 2015
 */
public class ObsSeriesImpl extends TimeSeriesImpl
{
    FoiTimesStoreImpl foiTimesStore;
    
    
    // default constructor needed by PERST on Android JVM
    @SuppressWarnings("unused")
    private ObsSeriesImpl() {}
    
    
    ObsSeriesImpl(Storage db, DataComponent recordDescription, DataEncoding recommendedEncoding)
    {
        super(db, recordDescription, recommendedEncoding);
        this.foiTimesStore = new FoiTimesStoreImpl(db);
    }
    
    
    Set<FoiTimePeriod> getFoiTimePeriods(IDataFilter filter)
    {
        // FOI ID list
        Collection<String> foiIDs = null;
        if (filter instanceof IObsFilter)
            foiIDs = ((IObsFilter)filter).getFoiIDs();
        Set<FoiTimePeriod> foiTimes = foiTimesStore.getSortedFoiTimes(foiIDs);
        
        // trim periods to filter time range if specified
        double[] timeRange = filter.getTimeStampRange();
        if (timeRange != null)
        {
            Iterator<FoiTimePeriod> it = foiTimes.iterator();
            while (it.hasNext())
            {
                FoiTimePeriod foiTime = it.next();
                
                // trim foi period to filter time range
                if (foiTime.timePeriod[0] < timeRange[0])
                    foiTime.timePeriod[0] = timeRange[0];
                
                if (foiTime.timePeriod[1] > timeRange[1])
                    foiTime.timePeriod[1] = timeRange[1];
                                
                // case period is completely outside of time range
                if (foiTime.timePeriod[0] > foiTime.timePeriod[1])
                    it.remove();
            }
        }
        
        // TODO FOI spatial filter
        
        return foiTimes;
    }


    @Override
    Iterator<DataBlock> getDataBlockIterator(final IDataFilter filter)
    {
        if (filter instanceof IObsFilter)
        {
            // FoI ID list
            final Set<FoiTimePeriod> foiTimePeriods = getFoiTimePeriods(filter);
                        
            if (foiTimePeriods != null && !foiTimePeriods.isEmpty())
            {
                // scan through each time range sequentially
                // but wrap the proces with a single iterator
                return new Iterator<DataBlock>()
                {
                    Iterator<FoiTimePeriod> periodIt = foiTimePeriods.iterator();
                    Iterator<DataBlock> recordIt;
                                        
                    public final boolean hasNext()
                    {
                        return periodIt.hasNext() || recordIt.hasNext();
                    }
        
                    public final DataBlock next()
                    {
                        if (recordIt == null || !recordIt.hasNext())
                        {
                            // process next time range
                            double[] timeRange = periodIt.next().timePeriod;
                            recordIt = recordIndex.iterator(new Key(timeRange[0]), new Key(timeRange[1]), Index.ASCENT_ORDER);
                        }
                        
                        // continue processing time range
                        return recordIt.next();
                    }
                
                    public final void remove()
                    {
                    }
                };
            }
        }

        return super.getDataBlockIterator(filter);
    }


    @Override
    Iterator<DBRecord> getRecordIterator(IDataFilter filter)
    {
        // FoI ID list
        final Set<FoiTimePeriod> foiTimePeriods = getFoiTimePeriods(filter);
                    
        // scan through each time range sequentially
        // but wrap the process with a single iterator
        return new Iterator<DBRecord>()
        {
            Iterator<FoiTimePeriod> periodIt = foiTimePeriods.iterator();
            Iterator<Entry<Object, DataBlock>> recordIt;
            String currentFoiID;
                                
            public final boolean hasNext()
            {
                return periodIt.hasNext() || (recordIt != null && recordIt.hasNext());
            }

            public final DBRecord next()
            {
                if (recordIt == null || !recordIt.hasNext())
                {
                    // process next time range
                    FoiTimePeriod nextPeriod = periodIt.next();
                    currentFoiID = nextPeriod.uid;
                    double[] timeRange = nextPeriod.timePeriod;
                    recordIt = recordIndex.entryIterator(new Key(timeRange[0]), new Key(timeRange[1]), Index.ASCENT_ORDER);
                }
                
                // continue processing time range
                Entry<Object, DataBlock> entry = recordIt.next();
                ObsKey key = new ObsKey(recordDescription.getName(), currentFoiID, (double)entry.getKey());
                return new DBRecord(key, entry.getValue());
            }
        
            public final void remove()
            {
                recordIt.remove();
            }
        };
    }


    @Override
    int getNumMatchingRecords(IDataFilter filter)
    {
        // TODO Auto-generated method stub
        return super.getNumMatchingRecords(filter);
    }


    @Override
    void store(DataKey key, DataBlock data)
    {
        super.store(key, data);
        
        if (key instanceof ObsKey)
        {
            // update FOI times
            String foiID = ((ObsKey)key).foiID;
            if (foiID != null)
            {
                double timeStamp = key.timeStamp;
                foiTimesStore.updateFoiPeriod(foiID, timeStamp);
            }
        }
    }


    @Override
    int remove(IDataFilter filter)
    {
        if (filter instanceof IObsFilter)
        {
            for (String foidID: ((IObsFilter)filter).getFoiIDs())
            {
                // completely remove FOI times if no more records will be left
                if (filter.getTimeStampRange() == null) // || time range contains all foi time ranges
                    foiTimesStore.remove(foidID);
            }
        }
        
        return super.remove(filter);
    }
}
