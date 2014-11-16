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

package org.sensorhub.impl.persistence.perst;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.ObsKey;
import org.sensorhub.api.persistence.StorageException;


/**
 * <p>
 * TODO ObservationStorageImpl type description
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 16, 2010
 */
public class ObservationStorageImpl implements IObsStorage<ObsStorageConfig>
{

    
    @Override
    public void init(ObsStorageConfig config)
    {
        // TODO Auto-generated method stub

    }
    
    
    @Override
    public void updateConfig(ObsStorageConfig config)
    {
        // TODO Auto-generated method stub        
    }    


    @Override
    public void start() throws StorageException
    {
        
    }
    
    
    @Override
    public void stop() throws StorageException
    {
        close();
    }
    
    
    @Override
    public void open() throws StorageException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void close() throws StorageException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public int getNumRecords()
    {
        return 0;
    }
    
    
    @Override
    public DataComponent getRecordDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public ObsKey store(ObsKey key, DataBlock data)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DataBlock getDataBlock(long id)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public IDataRecord<ObsKey> getRecord(long id)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<DataBlock> getDataBlocks(IObsFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<IDataRecord<ObsKey>> getRecords(IObsFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Iterator<DataBlock> getDataBlockIterator(IObsFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Iterator<IDataRecord<ObsKey>> getRecordIterator(IObsFilter filter)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public DataKey update(long id, ObsKey key, DataBlock data)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void remove(long id)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public int remove(IObsFilter filter)
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
    public void sync(IStorageModule<?> storage)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public boolean isAutoCommit()
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    
    @Override
    public void setAutoCommit(boolean autoCommit)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void commit()
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void rollback()
    {
        // TODO Auto-generated method stub

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


    @Override
    public boolean isEnabled()
    {
        return false;
    }
    
    
    @Override
    public ObsStorageConfig getConfiguration()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<String> getFeatureOfInterestIds()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getLocalID()
    {
        // TODO Auto-generated method stub
        return null;
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
    public void cleanup() throws StorageException
    {
        // TODO Auto-generated method stub        
    }
}
