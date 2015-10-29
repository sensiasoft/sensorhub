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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.garret.perst.Index;
import org.garret.perst.IterableIterator;
import org.garret.perst.Key;
import org.garret.perst.Storage;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.FeatureFilter;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IFeatureFilter;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.ObsKey;
import org.sensorhub.impl.persistence.perst.FoiTimesStoreImpl.FoiTimePeriod;
import com.vividsolutions.jts.geom.Polygon;


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
    
    
    static abstract class IteratorWithFoi extends IterableIterator<Entry<Object,DataBlock>>
    {
        public abstract String getCurrentFoiID();
    }
    
    
    // default constructor needed by PERST on Android JVM
    @SuppressWarnings("unused")
    private ObsSeriesImpl() {}
    
    
    ObsSeriesImpl(Storage db, DataComponent recordDescription, DataEncoding recommendedEncoding)
    {
        super(db, recordDescription, recommendedEncoding);
        this.foiTimesStore = new FoiTimesStoreImpl(db);
    }
    
    
    Set<FoiTimePeriod> getFoiTimePeriods(final IDataFilter filter)
    {
        // extract FOI filters if any
        Collection<String> foiIDs = null;
        Polygon roi = null;
        if (filter instanceof IObsFilter)
        {
            foiIDs = ((IObsFilter)filter).getFoiIDs();
            roi = ((IObsFilter) filter).getRoi();
        }
        
        // if using spatial filter, first get matching FOI IDs
        // and then follow normal process
        if (roi != null)
        {
            IFeatureFilter foiFilter = new FeatureFilter()
            {
                public Collection<String> getFeatureIDs()
                {
                    return ((IObsFilter)filter).getFoiIDs();
                }

                public Polygon getRoi()
                {
                    return ((IObsFilter) filter).getRoi();
                }
            };
            
            Iterator<String> foiIt = getFeatureStore().getFeatureIDs(foiFilter);
            Collection<String> allFoiIDs = new ArrayList<String>(100);
            if (foiIDs != null)
                allFoiIDs.addAll(foiIDs);
            while (foiIt.hasNext())
                allFoiIDs.add(foiIt.next());
            foiIDs = allFoiIDs;
        }
        
        // get time periods for list of FOIs
        Set<FoiTimePeriod> foiTimes = foiTimesStore.getSortedFoiTimes(foiIDs);
        
        // trim periods to filter time range if specified
        double[] timeRange = filter.getTimeStampRange();
        if (timeRange != null)
        {
            Iterator<FoiTimePeriod> it = foiTimes.iterator();
            while (it.hasNext())
            {
                FoiTimePeriod foiPeriod = it.next();
                
                // trim foi period to filter time range
                if (foiPeriod.start < timeRange[0])
                    foiPeriod.start = timeRange[0];
                
                if (foiPeriod.stop > timeRange[1])
                    foiPeriod.stop = timeRange[1];
                                
                // case period is completely outside of time range
                if (foiPeriod.start > foiPeriod.stop)
                    it.remove();
            }
        }
        
        return foiTimes;
    }


    @Override
    Iterator<DBRecord> getRecordIterator(IDataFilter filter)
    {
        // here, even when IObsFilter is not used we scan through FOIs time periods 
        // because we need to read the FOI ID anyway
        
        final IteratorWithFoi it = getEntryIterator(filter);
        
        return new Iterator<DBRecord>()
        {
            public final boolean hasNext()
            {
                return it.hasNext();
            }

            public final DBRecord next()
            {
                Entry<Object, DataBlock> entry = it.next();
                String currentFoiID = it.getCurrentFoiID();
                ObsKey key = new ObsKey(recordDescription.getName(), currentFoiID, (double)entry.getKey());
                return new DBRecord(key, entry.getValue());
            }

            public final void remove()
            {
                it.remove();
            }
        };
    }
    
    
    protected IteratorWithFoi getEntryIterator(IDataFilter filter)
    {
     // get time periods formatching FOIs
        final Set<FoiTimePeriod> foiTimePeriods = getFoiTimePeriods(filter);
            
        // scan through each time range sequentially
        // but wrap the process with a single iterator
        return new IteratorWithFoi()
        {
            Iterator<FoiTimePeriod> periodIt = foiTimePeriods.iterator();
            Iterator<Entry<Object, DataBlock>> recordIt;
            protected String currentFoiID;
                                
            public final boolean hasNext()
            {
                return periodIt.hasNext() || (recordIt != null && recordIt.hasNext());
            }

            public final Entry<Object,DataBlock> next()
            {
                if (recordIt == null || !recordIt.hasNext())
                {
                    // process next time range
                    FoiTimePeriod nextPeriod = periodIt.next();
                    currentFoiID = nextPeriod.uid;
                    recordIt = recordIndex.entryIterator(new Key(nextPeriod.start), new Key(nextPeriod.stop), Index.ASCENT_ORDER);
                }
                
                // continue processing time range
                return recordIt.next();
            }
        
            public final void remove()
            {
                recordIt.remove();
            }

            public String getCurrentFoiID()
            {
                return currentFoiID;
            }
        };
    }
    
    
    protected FeatureStoreImpl getFeatureStore()
    {
        return ((ObsStorageRoot)getStorage().getRoot()).featureStore;
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
