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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.junit.Test;
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
public abstract class AbstractTestObsStorage<StorageType extends IObsStorage<?>> extends AbstractTestBasicStorage<StorageType>
{
          
    @Test
    public void testStoreOneFoiAndGetMultipleRecordsByFoiID() throws Exception
    {
        DataBlock data;
        ObsKey key;
        
        final String recordType = "ds2";
        DataComponent recordDef = createDs2();
        
        // write N records
        final String foiID = "urn:domain:features:foi001";
        final double timeStep = 0.1;
        final int numRecords = 100;
        List<DataBlock> dataList = new ArrayList<DataBlock>(numRecords);
        storage.setAutoCommit(false);
        for (int i=0; i<numRecords; i++)
        {
            data = recordDef.createDataBlock();
            data.setDoubleValue(0, i - 0.3);
            data.setIntValue(1, 3*i);
            data.setStringValue(2, "testfilter" + i);
            dataList.add(data);
            key = new ObsKey(recordType, foiID, i*timeStep);
            storage.storeRecord(key, data);
        }
        storage.commit();
        forceReadBackFromStorage();
        
        // prepare filter
        IObsFilter filter = new ObsFilter(recordType) {
            public Set<String> getFoiIDs()
            {
                Set<String> foiSet = new HashSet<String>();
                foiSet.add(foiID);
                return foiSet;
            }
        };
        
        // retrieve data blocks and check their values
        int i = 0;
        Iterator<DataBlock> it1 = storage.getDataBlockIterator(filter);
        while (it1.hasNext())
        {
            TestUtils.assertEquals(dataList.get(i), it1.next());
            i++;
        }
    
        // retrieve records and check their values
        i = 0;
        Iterator<? extends IDataRecord> it2 = storage.getRecordIterator(filter);
        while (it2.hasNext())
        {
            TestUtils.assertEquals(dataList.get(i), it2.next().getData());
            i++;
        }
    }
    
    
    @Test
    public void testStoreMultipleFoisAndGetMultipleRecordsByFoiID() throws Exception
    {
        DataBlock data;
        ObsKey key;
        
        final String recordType = "ds2";
        DataComponent recordDef = createDs2();
        
        // write N records
        final String foi1 = "urn:domain:features:foi001";
        final String foi2 = "urn:domain:features:foi002";
        final String foi3 = "urn:domain:features:foi003";
        final double timeStep = 0.1;
        final int numRecords = 100;
        int foi2StartIndex = 20;
        int foi3StartIndex = 60;
        List<DataBlock> dataList = new ArrayList<DataBlock>(numRecords);
        storage.setAutoCommit(false);
        for (int i=0; i<numRecords; i++)
        {
            data = recordDef.createDataBlock();
            data.setDoubleValue(0, i - 0.3);
            data.setIntValue(1, 3*i);
            data.setStringValue(2, "testfilter" + i);
            dataList.add(data);
            String foiID = foi1;
            if (i >= foi3StartIndex)
                foiID = foi3;
            else if (i >= foi2StartIndex)
                foiID = foi2;
            key = new ObsKey(recordType, foiID, i*timeStep);
            storage.storeRecord(key, data);
        }
        storage.commit();
        forceReadBackFromStorage();
        
        // retrieve Foi1 records and check their values
        IObsFilter filter = new ObsFilter(recordType) {
            public Set<String> getFoiIDs()
            {
                Set<String> foiSet = new HashSet<String>();
                foiSet.add(foi1);
                return foiSet;
            }
        };
        
        int i = 0;
        Iterator<? extends IDataRecord> it = storage.getRecordIterator(filter);
        while (it.hasNext())
        {
            TestUtils.assertEquals(dataList.get(i), it.next().getData());
            i++;
        }
        assertEquals(foi2StartIndex, i);
    
        // retrieve Foi2 records and check their values
        filter = new ObsFilter(recordType) {
            public Set<String> getFoiIDs()
            {
                Set<String> foiSet = new HashSet<String>();
                foiSet.add(foi2);
                return foiSet;
            }
        };
        
        i = foi2StartIndex;
        it = storage.getRecordIterator(filter);
        while (it.hasNext())
        {
            TestUtils.assertEquals(dataList.get(i), it.next().getData());
            i++;
        }
        assertEquals(foi3StartIndex, i);
    }
    
}
