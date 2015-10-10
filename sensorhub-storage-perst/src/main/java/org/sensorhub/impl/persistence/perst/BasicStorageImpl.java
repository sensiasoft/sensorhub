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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.garret.perst.MappedFile;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;
import org.garret.perst.StorageFactory;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IRecordStorageModule;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageEvent;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.api.persistence.StorageEvent.Type;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * PERST implementation of {@link IBasicStorage} for storing simple data records.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 15, 2014
 */
public class BasicStorageImpl extends AbstractModule<BasicStorageConfig> implements IRecordStorageModule<BasicStorageConfig>
{          
    //private static final Logger log = LoggerFactory.getLogger(BasicStorageImpl.class);    
    
    protected Storage db;
    protected Persistent dbRoot;
    protected boolean autoCommit;
    
        
    @Override
    public synchronized void start() throws StorageException
    {
        try
        {
            this.autoCommit = true;
            
            // acquire file lock on DB file
            MappedFile dbFile = new MappedFile(config.storagePath, 100*1024, false);
            //OSFile dbFile = new OSFile(config.storagePath, false, false);
            if (!dbFile.tryLock(false))
                throw new StorageException("Storage file " + config.storagePath + " is already opened by another SensorHub process");
            
            db = StorageFactory.getInstance().createStorage();    
            db.setProperty("perst.concurrent.iterator", true);
            //db.setProperty("perst.alternative.btree", true);
            db.open(dbFile, config.memoryCacheSize*1024);
            dbRoot = (BasicStorageRoot)db.getRoot();
            
            if (dbRoot == null)
            { 
                dbRoot = createRoot(db);    
                db.setRoot(dbRoot);
            }
        }
        catch (Exception e)
        {
            throw new StorageException("Error while opening storage " + config.name, e);
        }
    }
    
    
    protected Persistent createRoot(Storage db)
    {
        return new BasicStorageRoot(db);
    }
    

    @Override
    public synchronized void stop() throws SensorHubException
    {
        if (db != null) 
        {
            db.close();
            db = null;
        }
    }


    @Override
    public synchronized void cleanup() throws SensorHubException
    {
        if (db != null)
            stop();
        
        // we just mark file as deleted by renaming it with .deleted suffix
        // storage will restart with an empty file but we don't loose any data
        if (config.storagePath != null)
        {
            File dbFile = new File(config.storagePath);
            File newFile = new File(config.storagePath + ".deleted");
            dbFile.renameTo(newFile);
        }
    }
    
    
    @Override
    public synchronized void backup(OutputStream os) throws IOException
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
    public synchronized void commit()
    {
        db.commit();
    }


    @Override
    public synchronized void rollback()
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
        return ((BasicStorageRoot)dbRoot).getLatestDataSourceDescription();
    }


    @Override
    public List<AbstractProcess> getDataSourceDescriptionHistory(double startTime, double endTime)
    {
        return ((BasicStorageRoot)dbRoot).getDataSourceDescriptionHistory(startTime, endTime);
    }


    @Override
    public AbstractProcess getDataSourceDescriptionAtTime(double time)
    {
        return ((BasicStorageRoot)dbRoot).getDataSourceDescriptionAtTime(time);
    }


    @Override
    public synchronized void storeDataSourceDescription(AbstractProcess process)
    {
        ((BasicStorageRoot)dbRoot).storeDataSourceDescription(process);        
        if (autoCommit)
            commit();
    }


    @Override
    public synchronized void updateDataSourceDescription(AbstractProcess process)
    {
        ((BasicStorageRoot)dbRoot).updateDataSourceDescription(process);
        if (autoCommit)
            commit();
    }


    @Override
    public synchronized void removeDataSourceDescription(double time)
    {
        ((BasicStorageRoot)dbRoot).removeDataSourceDescription(time);        
        if (autoCommit)
            commit();
    }


    @Override
    public synchronized void removeDataSourceDescriptionHistory(double startTime, double endTime)
    {
        ((BasicStorageRoot)dbRoot).removeDataSourceDescriptionHistory(startTime, endTime);        
        if (autoCommit)
            commit();
    }
    
    
    @Override
    public synchronized void addRecordStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding)
    {
        ((BasicStorageRoot)dbRoot).addRecordStore(name, recordStructure, recommendedEncoding);
        if (autoCommit)
            commit();
    }
    
    
    @Override
    public Map<String, ? extends IRecordStoreInfo> getRecordStores()
    {
        return ((BasicStorageRoot)dbRoot).getRecordStores();
    }


    @Override
    public int getNumRecords(String recordType)
    {
        return ((BasicStorageRoot)dbRoot).getNumRecords(recordType);
    }

    
    @Override
    public double[] getRecordsTimeRange(String recordType)
    {
        return ((BasicStorageRoot)dbRoot).getRecordsTimeRange(recordType);
    }
    
    
    @Override
    public Iterator<double[]> getRecordsTimeClusters(String recordType)
    {
        return ((BasicStorageRoot)dbRoot).getRecordsTimeClusters(recordType);
    }
    
    
    @Override
    public DataBlock getDataBlock(DataKey key)
    {
        return ((BasicStorageRoot)dbRoot).getDataBlock(key);
    }


    @Override
    public Iterator<DataBlock> getDataBlockIterator(IDataFilter filter)
    {
        return ((BasicStorageRoot)dbRoot).getDataBlockIterator(filter);
    }


    @Override
    public Iterator<? extends IDataRecord> getRecordIterator(IDataFilter filter)
    {
        return ((BasicStorageRoot)dbRoot).getRecordIterator(filter);
    }


    @Override
    public int getNumMatchingRecords(IDataFilter filter)
    {
        return ((BasicStorageRoot)dbRoot).getNumMatchingRecords(filter);
    }
    

    @Override
    public synchronized void storeRecord(DataKey key, DataBlock data)
    {
        ((BasicStorageRoot)dbRoot).storeRecord(key, data);        
        if (autoCommit)
            commit();
        
        eventHandler.publishEvent(new StorageEvent(System.currentTimeMillis(), this, key.recordType, Type.STORE));
    }


    @Override
    public synchronized void updateRecord(DataKey key, DataBlock data)
    {
        ((BasicStorageRoot)dbRoot).updateRecord(key, data);
        if (autoCommit)
            commit();
        
        eventHandler.publishEvent(new StorageEvent(System.currentTimeMillis(), this, key.recordType, Type.UPDATE));
    }


    @Override
    public synchronized void removeRecord(DataKey key)
    {
        ((BasicStorageRoot)dbRoot).removeRecord(key);
        if (autoCommit)
            commit();
        
        eventHandler.publishEvent(new StorageEvent(System.currentTimeMillis(), this, key.recordType, Type.DELETE));
    }


    @Override
    public synchronized int removeRecords(IDataFilter filter)
    {
        int count = ((BasicStorageRoot)dbRoot).removeRecords(filter);
        if (autoCommit)
            commit();
        
        eventHandler.publishEvent(new StorageEvent(System.currentTimeMillis(), this, filter.getRecordType(), Type.DELETE));
        return count;
    }
}
