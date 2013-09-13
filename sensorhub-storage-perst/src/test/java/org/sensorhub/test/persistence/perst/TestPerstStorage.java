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

package org.sensorhub.test.persistence.perst;

import java.io.File;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.impl.persistence.perst.BasicStorageConfig;
import org.sensorhub.impl.persistence.perst.BasicStorageImpl;
import org.vast.cdm.common.DataBlock;
import org.vast.data.DataBlockDouble;
import junit.framework.TestCase;


public class TestPerstStorage extends TestCase
{
    static final String p1 = "sensor1";
    static final String p2 = "sensor2";
    static final long t1 = 1000;
    static final long t2 = 2000;
    
    
    @SuppressWarnings("rawtypes")
    protected IBasicStorage createNewBasicStorage() throws Exception
    {
        BasicStorageConfig config = new BasicStorageConfig();
        config.memoryCacheSize = 0;
        File dbFile = File.createTempFile("testdb", ".dat");
        dbFile.deleteOnExit();
        config.storagePath = dbFile.getAbsolutePath();        
        IBasicStorage db = new BasicStorageImpl();
        db.init(config);
        return db;
    }
    
    
    public void testCreateAndInitStorage() throws Exception
    {
        createNewBasicStorage();
    }
    
    
    public void testBasicStorage() throws Exception
    {
        @SuppressWarnings("rawtypes")
        IBasicStorage db = createNewBasicStorage();
        
        try
        {            
            db.open();
            
            // create data block
            int blockSize = 10;
            DataBlock data = new DataBlockDouble(blockSize);
            for (int i=0; i<blockSize; i++)
                data.setDoubleValue(i, i);
            
            // store 2 records in DB
            DataKey key;
            key = db.store(new DataKey(p1, t1), data);
            assertTrue(key.recordID == 1);            
            key = db.store(new DataKey(p2, t2), data);
            assertTrue(key.recordID == 2);
            
            // read back records
            IDataRecord<DataKey> rec;
            rec = db.getRecord(1);
            assertTrue(rec.getKey().producerID.equals(p1));
            assertTrue(rec.getKey().timeStamp == t1);
            rec = db.getRecord(2);
            assertTrue(rec.getKey().producerID.equals(p2));
            assertTrue(rec.getKey().timeStamp == t2);
        }
        finally
        {
            db.close();
        }
    }
    
    
    /*public void testReadBackStorage() throws Exception
    {
        IBasicStorage db = createNewBasicStorage();
        
        try
        {            
            db.open();
            
            // read back record
            IDataRecord<DataKey> rec = db.getRecord(1);
            assertTrue(rec.getKey().producerID.equals(p1));
        }
        finally
        {
            db.close();
        }
    }*/
}
