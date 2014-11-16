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

package org.sensorhub.impl.persistence;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.persistence.StorageException;


/**
 * <p>
 * In-memory storage for testing sensor storage helper
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Nov 8, 2013
 */
public class InMemoryBasicStorage implements IBasicStorage<StorageConfig>
{
    StorageConfig config;
    DataComponent dataDescription;
    List<IDataRecord<DataKey>> recordList;
    
    
    public class DBRecord implements IDataRecord<DataKey>
    {
        DataKey key;
        DataBlock data;
        
        
        public DBRecord(DataKey key, DataBlock data)
        {
            this.key = key;
            this.data = data;
        }


        @Override
        public DataKey getKey()
        {
            return key;
        }
        

        @Override
        public DataBlock getData()
        {
            return data;
        }

    }
    
    
    @Override
    public void open() throws StorageException
    {
        recordList = new LinkedList<IDataRecord<DataKey>>();
    }


    @Override
    public void close() throws StorageException
    {        
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return dataDescription;
    }
    
    
    @Override
    public int getNumRecords()
    {
        return recordList.size();
    }
    
    
    @Override
    public List<String> getProducerIDs()
    {
        List<String> ids = new ArrayList<String>();
        
        Iterator<IDataRecord<DataKey>> it = recordList.iterator();
        while (it.hasNext())
        {
            String producerID = it.next().getKey().producerID;
            if (producerID != null)
                ids.add(producerID);
        }
        
        return ids;
    }


    @Override
    public long[] getDataTimeRange()
    {
        long[] period = new long[] { Long.MAX_VALUE, Long.MIN_VALUE};
        
        Iterator<IDataRecord<DataKey>> it = recordList.iterator();
        while (it.hasNext())
        {
            long timeStamp = it.next().getKey().timeStamp;
            if (timeStamp < period[0])
                period[0] = timeStamp;
            if (timeStamp > period[1])
                period[1] = timeStamp;
        }
        
        return period;
    }


    @Override
    public long[] getTimeRangeForProducer(String selectedProducerID)
    {
        long[] period = new long[] { Long.MAX_VALUE, Long.MIN_VALUE};
        
        Iterator<IDataRecord<DataKey>> it = recordList.iterator();
        while (it.hasNext())
        {
            IDataRecord<DataKey> rec = it.next();
            String producerID = rec.getKey().producerID;
            long timeStamp = rec.getKey().timeStamp;
            
            if (producerID != null && producerID.equals(selectedProducerID))
            {
                if (timeStamp < period[0])
                    period[0] = timeStamp;
                if (timeStamp > period[1])
                    period[1] = timeStamp;
            }
        }
        
        return period;
    }


    @Override
    public DataKey store(DataKey key, DataBlock data)
    {
        recordList.add(new DBRecord(key, data));
        return key;
    }


    @Override
    public DataBlock getDataBlock(long id)
    {
        IDataRecord<?> rec = getRecord(id);
        return rec.getData();
    }

    
    @Override
    public List<DataBlock> getDataBlocks(IDataFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public IDataRecord<DataKey> getRecord(long id)
    {
        Iterator<IDataRecord<DataKey>> it = recordList.iterator();
        while (it.hasNext())
        {
            IDataRecord<DataKey> rec = it.next();
            if (rec.getKey().recordID == id)
                return rec;
        }
        
        return null;
    }


    @Override
    public List<IDataRecord<DataKey>> getRecords(IDataFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }

    
    @Override
    public Iterator<IDataRecord<DataKey>> getRecordIterator(IDataFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DataKey update(long id, DataKey key, DataBlock data)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void remove(long id)
    {
        ListIterator<IDataRecord<DataKey>> it = recordList.listIterator();
        while (it.hasNext())
        {
            IDataRecord<DataKey> rec = it.next();
            if (rec.getKey().recordID == id)
                it.remove();
        }
    }


    @Override
    public int remove(IDataFilter filter)
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public void backup(OutputStream os)
    {
    }


    @Override
    public void restore(InputStream is)
    {
    }


    @Override
    public void sync(IStorageModule<?> storage)
    {
    }


    @Override
    public void setAutoCommit(boolean autoCommit)
    {
    }
    
    
    @Override
    public boolean isAutoCommit()
    {
        return false;
    }
    

    @Override
    public void commit()
    {
    }


    @Override
    public void rollback()
    {
    }


    @Override
    public boolean isEnabled()
    {
        return config.enabled;
    }


    @Override
    public void init(StorageConfig config) throws SensorHubException
    {
        this.config = config;        
    }


    @Override
    public void updateConfig(StorageConfig config) throws SensorHubException
    {
        this.config = config;
    }
    
    
    @Override
    public void start() throws SensorHubException
    {        
        open();
    }
    
    
    @Override
    public void stop() throws SensorHubException
    {        
        close();
    }


    @Override
    public StorageConfig getConfiguration()
    {
        return config;
    }


    @Override
    public String getName()
    {
        return config.name;
    }


    @Override
    public String getLocalID()
    {
        return config.id;
    }


    @Override
    public void saveState(IModuleStateSaver saver) throws SensorHubException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void loadState(IModuleStateLoader loader) throws SensorHubException
    {
        // TODO Auto-generated method stub
    }
    
    
    @Override
    public void cleanup() throws StorageException
    {
        recordList.clear();
    }    


    @Override
    public void registerListener(IEventListener listener)
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        // TODO Auto-generated method stub
    }

}
