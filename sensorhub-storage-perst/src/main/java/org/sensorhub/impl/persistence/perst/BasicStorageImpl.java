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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.opengis.gml.v32.AbstractTimeGeometricPrimitive;
import net.opengis.gml.v32.TimeInstant;
import net.opengis.gml.v32.TimePeriod;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.garret.perst.Index;
import org.garret.perst.Key;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;
import org.garret.perst.StorageFactory;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.ITimeSeriesDataStore;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.sensorhub.impl.module.AbstractModule;


/**
 * <p>
 * Basic implementation of a PERST based persistent storage of data records.
 * This class must be listed in the META-INF services folder to be available via the persistence manager.
 * </p>
 *
 * <p>Copyright (c) 2014</p>
 * @author Alexandre Robin
 * @since Nov 15, 2014
 */
public class BasicStorageImpl extends AbstractModule<BasicStorageConfig> implements IBasicStorage<BasicStorageConfig>
{
    private static Key KEY_SML_START_ALL_TIME = new Key(Double.NEGATIVE_INFINITY);
    private static Key KEY_SML_END_ALL_TIME = new Key(Double.POSITIVE_INFINITY);
    static Key KEY_DATA_START_ALL_TIME = new Key(new Object[] {Double.NEGATIVE_INFINITY});
    static Key KEY_DATA_END_ALL_TIME = new Key(new Object[] {Double.POSITIVE_INFINITY});
    
    protected Storage db;
    protected DBRoot dbRoot;
    protected boolean autoCommit;
    
    
    /*
     * Default constructor necessary for java service loader
     */
    public BasicStorageImpl()
    {
    }
    
    
    @Override
    public void start() throws StorageException
    {
        try
        {
            this.autoCommit = true;
            
            // first make sure it's not already opened
            if (db != null && db.isOpened())
                throw new StorageException("Storage " + getLocalID() + " is already opened");
            
            db = StorageFactory.getInstance().createStorage();            
            db.open(config.storagePath, config.memoryCacheSize*1024);
            dbRoot = (DBRoot)db.getRoot();
            
            if (dbRoot == null)
            { 
                dbRoot = new DBRoot();                
                db.setRoot(dbRoot);
            }
            
            // make sure all data stores have event handlers
            // HACK because transient variable is not recreated when loading from existing DB
            for (TimeSeriesImpl timeSeries: dbRoot.dataStores.values())
            {
                timeSeries.eventHandler = new BasicEventHandler();
                timeSeries.parentStorage = this;
            }
        }
        catch (Exception e)
        {
            throw new StorageException("Error while opening storage " + config.name, e);
        }
    }


    @Override
    public void stop() throws SensorHubException
    {
        db.close();
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        // remove database file?
    }
    
    
    @Override
    public void backup(OutputStream os) throws IOException
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
    public final void commit()
    {
        db.commit();
    }


    @Override
    public void rollback()
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
        Iterator<AbstractProcess> it = dbRoot.descriptionTimeIndex.iterator(new Key(-Double.MAX_VALUE), new Key(Double.MAX_VALUE), Index.DESCENT_ORDER);
        if (it.hasNext())
            return it.next();
        return null;
    }


    @Override
    public List<AbstractProcess> getDataSourceDescriptionHistory()
    {
        return Collections.unmodifiableList(dbRoot.descriptionTimeIndex.getList(KEY_SML_START_ALL_TIME, KEY_SML_END_ALL_TIME));
    }


    @Override
    public AbstractProcess getDataSourceDescriptionAtTime(double time)
    {
        Iterator<AbstractProcess> it = dbRoot.descriptionTimeIndex.iterator(KEY_SML_START_ALL_TIME, new Key(time), Index.DESCENT_ORDER);
        if (it.hasNext())
            return it.next();
        return null;
    }


    @Override
    public void storeDataSourceDescription(AbstractProcess process) throws StorageException
    {
        // we add the description in index for each validity period/instant
        for (AbstractTimeGeometricPrimitive validTime: process.getValidTimeList())
        {
            double time = Double.NaN;
            
            try
            {
                if (validTime instanceof TimeInstant)
                    time = ((TimeInstant) validTime).getTimePosition().getDecimalValue();
                else if (validTime instanceof TimePeriod)
                    time = ((TimePeriod) validTime).getBeginPosition().getDecimalValue();
            }
            catch (Exception e)
            {
                throw new StorageException("Sensor description must contain at least one validity period");
            }
            
            if (!Double.isNaN(time))
                dbRoot.descriptionTimeIndex.put(new Key(time), process);
        }
        
        if (autoCommit)
            commit();
    }


    @Override
    public void updateDataSourceDescription(AbstractProcess process) throws StorageException
    {
        // TODO Auto-generated method stub
        
        //db.deallocate(oldObject);
        if (autoCommit)
            commit();
    }


    @Override
    public void removeDataSourceDescription(double time)
    {
        Iterator<AbstractProcess> it = dbRoot.descriptionTimeIndex.iterator(KEY_SML_START_ALL_TIME, new Key(time), Index.DESCENT_ORDER);
        if (it.hasNext())
        {
            AbstractProcess sml = it.next();
            it.remove();
            db.deallocate(sml);
        }
        
        if (autoCommit)
            commit();
    }


    @Override
    public void removeDataSourceDescriptionHistory()
    {
        Iterator<AbstractProcess> it = dbRoot.descriptionTimeIndex.iterator(KEY_SML_START_ALL_TIME, KEY_SML_END_ALL_TIME, Index.ASCENT_ORDER);
        while (it.hasNext())
        {
            AbstractProcess sml = it.next();
            it.remove();
            db.deallocate(sml);
        }
        
        if (autoCommit)
            commit();
    }


    @Override
    public Map<String, ? extends ITimeSeriesDataStore<IDataFilter>> getDataStores()
    {
        return Collections.unmodifiableMap(dbRoot.dataStores);
    }


    @Override
    public ITimeSeriesDataStore<IDataFilter> addNewDataStore(String name, DataComponent recordStructure, DataEncoding recommendedEncoding) throws StorageException
    {
        TimeSeriesImpl newTimeSeries = new TimeSeriesImpl(this, recordStructure, recommendedEncoding);
        dbRoot.dataStores.put(name, newTimeSeries);
        if (autoCommit)
            commit();
        return newTimeSeries;
    }

    
    /*
     * Root of storage
     */
    private class DBRoot extends Persistent
    {
        Index<AbstractProcess> descriptionTimeIndex;
        Map<String, TimeSeriesImpl> dataStores;
        
        public DBRoot()
        {
            dataStores = db.<String,TimeSeriesImpl>createMap(String.class, 10);
            descriptionTimeIndex = db.<AbstractProcess>createIndex(double.class, true);
        }
    }

}
