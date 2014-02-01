/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.garret.perst.Index;
import org.garret.perst.Key;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;
import org.garret.perst.StorageFactory;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IDataStorage;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;


/**
 * <p>
 * Basic implementation of a PERST based persistent storage of data records.
 * This class must be listed in the META-INF services folder to be available via the persistence manager.
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 15, 2010
 */
public class BasicStorageImpl implements IBasicStorage<BasicStorageConfig>
{
    protected BasicStorageConfig config;
    protected IEventHandler eventHandler;
    protected Storage db;
    protected DBRoot dbRoot;
    protected boolean autoCommit;
    
    
    protected class DBRoot extends Persistent
    {
        Index<DBRecord> indexById;
        Index<DBRecord> indexByProducerThenTime;
        Index<DBRecord> indexByTimeThenProducer;
        DataComponent recordDescription;
        long recordCount;
    }
    
    
    protected class DBRecord extends Persistent implements IDataRecord<DataKey>
    {
        protected DataKey key;
        protected DataBlock value;
        
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
     * Default constructor necessary for java service loader
     */
    public BasicStorageImpl()
    {
        this.eventHandler = new BasicEventHandler();
    }
    
    
    @Override
    public boolean isEnabled()
    {
        return config.enabled;
    }
    
    
    @Override
    public void init(BasicStorageConfig config)
    {
        this.config = config;
        this.autoCommit = true;
        
        if (db != null && db.isOpened())
            db.close();
        
        db = StorageFactory.getInstance().createStorage();
    }
    
    
    @Override
    public void updateConfig(BasicStorageConfig config)
    {
        // TODO Auto-generated method stub        
    }


    @Override
    public void open() throws StorageException
    {
        try
        {
            db.open(config.storagePath, config.memoryCacheSize*1024);
            dbRoot = (DBRoot)db.getRoot();
            
            if (dbRoot == null)
            { 
                dbRoot = new DBRoot();
                dbRoot.indexById = db.<DBRecord>createIndex(long.class, true);
                dbRoot.indexByProducerThenTime = db.<DBRecord>createIndex(new Class[] {String.class, Long.class}, true);
                dbRoot.indexByTimeThenProducer = db.<DBRecord>createIndex(new Class[] {Long.class, String.class}, true);
                db.setRoot(dbRoot);
            }
        }
        catch (Exception e)
        {
            throw new StorageException("Error while opening storage " + config.name, e);
        }
    }
    
    
    @Override
    public void close() throws StorageException
    {
        db.close();
    }


    @Override
    public BasicStorageConfig getConfiguration()
    {
        return config;
    }



    @Override
    public DataComponent getDataDescription()
    {
        return this.dbRoot.recordDescription;
    }


    @Override
    public List<String> getProducerIDs()
    {
        List<String> producers = new ArrayList<String>();
        
        /*Iterator<DBRecord> it = dbRoot.indexByProducerThenTime.iterator();
        while (it.hasNext())
        {
            String producerID = it.next().getKey().producerID;
        }*/
        
        return producers;
    }


    @Override
    public long[] getDataTimeRange()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public long[] getTimeRangeForProducer(String producerID)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DataKey store(DataKey key, DataBlock data)
    {
        DBRecord record = new DBRecord(key, data);
        boolean ok;
        
        try
        {
            // assign next record ID
            dbRoot.recordCount++;
            key.recordID = dbRoot.recordCount;            
            dbRoot.modify();
            
            // insert in ID index
            ok = dbRoot.indexById.put(key.recordID, record);
            if (!ok)
                throw new IllegalArgumentException("Record with id = " + key.recordID + " already exists in DB");
            
            // insert in producer/time stamp indexes
            ok = dbRoot.indexByProducerThenTime.put(new Key(new Object[] {key.producerID, key.timeStamp}), record);
            ok = dbRoot.indexByTimeThenProducer.put(new Key(new Object[] {key.timeStamp, key.producerID}), record);
            if (!ok)
                throw new IllegalArgumentException("Record with producer id = " + key.producerID + " and time stamp = " + key.timeStamp + " already exists in DB");
        }
        catch (IllegalArgumentException e)
        {
            db.rollback();
            dbRoot.recordCount--;
            throw e;
        }
        
        // commit changes if in auto mode
        if (autoCommit)
            db.commit();

        return key;
    }


    @Override
    public DataBlock getDataBlock(long id)
    {
        DBRecord record = dbRoot.indexById.get(id);
        return record.value;
    }


    @Override
    public IDataRecord<DataKey> getRecord(long id)
    {
        return dbRoot.indexById.get(id);
    }


    @Override
    public List<DataBlock> getDataBlocks(IDataFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<IDataRecord<DataKey>> getRecords(IDataFilter filter)
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
        DBRecord record = dbRoot.indexById.remove(new Key(id));
        DataKey key = record.key;
        dbRoot.indexByProducerThenTime.remove(new Key(new Object[] {key.producerID, key.timeStamp}));
        dbRoot.indexByTimeThenProducer.remove(new Key(new Object[] {key.timeStamp, key.producerID}));
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
        // TODO Auto-generated method stub

    }


    @Override
    public void restore(InputStream is)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void sync(IDataStorage<DataKey, ?, ?> storage)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void setAutoCommit(boolean autoCommit)
    {
        this.autoCommit = autoCommit;
    }


    @Override
    public void commit()
    {
        db.commit();
    }


    @Override
    public void rollback()
    {
        db.rollback();
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


    @Override
    public void saveState(IModuleStateSaver saver)
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void loadState(IModuleStateLoader loader)
    {
        // TODO Auto-generated method stub
        
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
    public void cleanup() throws StorageException
    {
        close();
        // TODO remove data on disk
    }

}
