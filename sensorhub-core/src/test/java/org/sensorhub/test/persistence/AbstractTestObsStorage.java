/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.junit.Test;
import org.sensorhub.api.persistence.IRecordStorageModule;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.ObsFilter;
import org.sensorhub.api.persistence.ObsKey;
import org.sensorhub.test.TestUtils;


/**
 * <p>
 * Abstract base for testing implementations of {@link IObsStorage}.
 * The storage needs to be correctly instianted by derived tests in a method
 * tagged with '@Before'.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <StorageType> type of storage under test
 * @since May 5, 2015
 */
public abstract class AbstractTestObsStorage<StorageType extends IRecordStorageModule<?>> extends AbstractTestBasicStorage<StorageType>
{
    static String[] FOI_SET1_IDS = new String[]
    {
        "urn:domain:features:foi001",
        "urn:domain:features:foi002",
        "urn:domain:features:foi003"
    };
    
    static int[] FOI_SET1_STARTS = new int[] {0, 20, 60};
    
    
    String[] FOI_IDS = FOI_SET1_IDS;
    int[] FOI_STARTS = FOI_SET1_STARTS;
    
    
    
    protected List<DataBlock> addObservationsWithFoiToStorage(DataComponent recordDef) throws Exception
    {
        // write N records
        final double timeStep = 0.1;
        final int numRecords = 100;
        List<DataBlock> dataList = new ArrayList<DataBlock>(numRecords);
        storage.setAutoCommit(false);
        for (int i=0; i<numRecords; i++)
        {
            DataBlock data = recordDef.createDataBlock();
            data.setDoubleValue(0, i*0.1);
            data.setIntValue(1, 3*i);
            data.setStringValue(2, "test" + i);
            dataList.add(data);
            String foiID = null;
            for (int f=0; f<FOI_IDS.length; f++)
            {
                if (i >= FOI_STARTS[f])
                    foiID = FOI_IDS[f];
            }
            ObsKey key = new ObsKey(recordDef.getName(), foiID, i*timeStep);
            storage.storeRecord(key, data);
        }
        
        storage.commit();
        forceReadBackFromStorage();
        
        return dataList;
    }
    
    
    protected IObsFilter buildFilterByFoiID(DataComponent recordDef, List<DataBlock> dataList, final int[] foiIndexes)
    {
        final Set<String> foiSet = new HashSet<String>();
        for (int index: foiIndexes)
            foiSet.add(FOI_IDS[index]);
        
        // generate filter
        IObsFilter filter = new ObsFilter(recordDef.getName()) {
            public Set<String> getFoiIDs() { return foiSet; }
        };
        
        // filter dataList to provide ground truth
        int i = 0;
        Iterator<DataBlock> it = dataList.iterator();
        while (it.hasNext())
        {
            it.next();
            boolean foundFoi = false;
            
            // check foi index ranges
            for (int index: foiIndexes)
            {
                int startIndex = FOI_STARTS[index];
                int endIndex = (index == FOI_IDS.length-1) ? 100 : FOI_STARTS[index+1];
                
                if (i >= startIndex && i <= endIndex-1)
                {
                    foundFoi = true;
                    break;
                }
            }
            
            if (!foundFoi)
                it.remove();
            
            i++;
        }
        
        return filter;
    }
    
    
    protected IObsFilter buildFilterByFoiIDAndTime(DataComponent recordDef, List<DataBlock> dataList, int[] foiIndexes, final double[] timeRange)
    {
        final Set<String> foiSet = new HashSet<String>();
        for (int index: foiIndexes)
            foiSet.add(FOI_IDS[index]);
        
        // generate filter
        IObsFilter filter = new ObsFilter(recordDef.getName()) {
            public double[] getTimeStampRange() { return timeRange; }
            public Set<String> getFoiIDs() { return foiSet; }
        };
        
        // filter dataList to provide ground truth
        int i = 0;
        Iterator<DataBlock> it = dataList.iterator();
        while (it.hasNext())
        {
            DataBlock data = it.next();
            double timeStamp = data.getDoubleValue(0);
            boolean foundFoi = false;
            
            // remove if not this FOI record or if not within time range
            // check foi index ranges
            for (int index: foiIndexes)
            {
                int startIndex = FOI_STARTS[index];
                int endIndex = (index == FOI_IDS.length-1) ? 100 : FOI_STARTS[index+1];
                
                if (i >= startIndex && i <= endIndex-1)
                {
                    foundFoi = true;
                    break;
                }
            }
            
            if (!foundFoi || timeStamp < timeRange[0] || timeStamp > timeRange[1])
                it.remove();
            
            i++;
        }
        
        return filter;
    }
    
    
    protected void checkFilteredResults(IObsFilter filter, List<DataBlock> dataList) throws Exception
    {
        int i;
        
        // check data blocks
        i = 0;
        Iterator<DataBlock> it1 = storage.getDataBlockIterator(filter);
        while (it1.hasNext())
        {
            TestUtils.assertEquals(dataList.get(i), it1.next());
            i++;
        }
        
        assertEquals("Wrong number of records", dataList.size(), i);
        
        // check full DB records
        i = 0;
        Iterator<? extends IDataRecord> it2 = storage.getRecordIterator(filter);
        while (it2.hasNext())
        {
            IDataRecord dbRec = it2.next();
            TestUtils.assertEquals(dataList.get(i), dbRec.getData());
            assertTrue(filter.getFoiIDs().contains(((ObsKey)dbRec.getKey()).foiID));
            i++;
        }
        
        assertEquals("Wrong number of records", dataList.size(), i);
    }
    
    
    @Test
    public void testStoreOneFoiAndGetRecordsByFoiID() throws Exception
    {
        FOI_IDS = new String[] {"urn:domain:features:myfoi"};
        FOI_STARTS = new int[1];
        DataComponent recordDef = createDs2();
        List<DataBlock> dataList = addObservationsWithFoiToStorage(recordDef);
        
        int[] foiIndex = new int[] { 0 };
        IObsFilter filter = buildFilterByFoiID(recordDef, dataList, foiIndex);
        checkFilteredResults(filter, dataList);
    }
    
    
    @Test
    public void testGetRecordsForOneFoiID() throws Exception
    {
        DataComponent recordDef = createDs2();
        List<DataBlock> dataList = addObservationsWithFoiToStorage(recordDef);
        List<DataBlock> testList = new ArrayList<DataBlock>(dataList.size());
        
        for (int foiIndex = 0; foiIndex < FOI_IDS.length; foiIndex++)
        {
            testList.clear();
            testList.addAll(dataList);
            IObsFilter filter = buildFilterByFoiID(recordDef, testList, new int[] {foiIndex});
            checkFilteredResults(filter, testList);
        }
    }
    
    
    @Test
    public void testGetRecordsForMultipleFoiIDs() throws Exception
    {
        DataComponent recordDef = createDs2();
        List<DataBlock> dataList = addObservationsWithFoiToStorage(recordDef);
        List<DataBlock> testList = new ArrayList<DataBlock>(dataList.size());
        
        // FOI 1 and 2
        testList.clear();
        testList.addAll(dataList);
        IObsFilter filter = buildFilterByFoiID(recordDef, testList, new int[] {0, 1});
        checkFilteredResults(filter, testList);
        
        // FOI 1 and 3
        testList.clear();
        testList.addAll(dataList);
        filter = buildFilterByFoiID(recordDef, testList, new int[] {0, 2});
        checkFilteredResults(filter, testList);
        
        // FOI 3 and 2
        testList.clear();
        testList.addAll(dataList);
        filter = buildFilterByFoiID(recordDef, testList, new int[] {2, 1});
        checkFilteredResults(filter, testList);
        
        // FOI 2, 1 and 3
        testList.clear();
        testList.addAll(dataList);
        filter = buildFilterByFoiID(recordDef, testList, new int[] {1, 0, 2});
        checkFilteredResults(filter, testList);
    }
    
    
    @Test
    public void testGetRecordsForOneFoiIDAndTime() throws Exception
    {
        DataComponent recordDef = createDs2();
        List<DataBlock> dataList = addObservationsWithFoiToStorage(recordDef);
        List<DataBlock> testList = new ArrayList<DataBlock>(dataList.size());
        
        double[] timeRange1 = new double[] {1.0, 8.4};
        
        // test FOI 1 by 1
        for (int foiIndex = 0; foiIndex < FOI_IDS.length; foiIndex++)
        {
            testList.clear();
            testList.addAll(dataList);
            IObsFilter filter = buildFilterByFoiIDAndTime(recordDef, testList, new int[] {foiIndex}, timeRange1);
            checkFilteredResults(filter, testList);
        }
    }
    
    
    @Test
    public void testGetRecordsForMultipleFoiIDsAndTime() throws Exception
    {
        DataComponent recordDef = createDs2();
        List<DataBlock> dataList = addObservationsWithFoiToStorage(recordDef);
        List<DataBlock> testList = new ArrayList<DataBlock>(dataList.size());
        double[] timeRange;
        IObsFilter filter;
        
        // FOI 1 and 2
        timeRange = new double[] {1.0, 8.4};
        testList.clear();
        testList.addAll(dataList);
        filter = buildFilterByFoiIDAndTime(recordDef, testList, new int[] {0, 1}, timeRange);
        checkFilteredResults(filter, testList);
        
        // FOI 3 and 2
        timeRange = new double[] {2.5, 8.4};
        testList.clear();
        testList.addAll(dataList);
        filter = buildFilterByFoiIDAndTime(recordDef, testList, new int[] {2, 1}, timeRange);
        checkFilteredResults(filter, testList);
    }
    
}
