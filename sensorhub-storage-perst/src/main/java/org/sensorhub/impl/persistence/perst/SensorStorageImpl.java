/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.persistence.ISensorDescriptionStorage;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.persistence.StorageException;


/**
 * <p>
 * PERST based implementation of SensorML document storage.
 * Sensor descriptions are stored either in the form of XML document
 * or instances of SMLProcess or derived classes.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Mar 6, 2014
 */
public class SensorStorageImpl implements ISensorDescriptionStorage<StorageConfig>
{

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
    public void cleanup() throws StorageException
    {
        // TODO Auto-generated method stub
        
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
    public void setAutoCommit(boolean autoCommit)
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
    public void sync(IStorageModule<?> storage)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isEnabled()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void init(StorageConfig config) throws SensorHubException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateConfig(StorageConfig config) throws SensorHubException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void start() throws SensorHubException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void stop() throws SensorHubException
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public StorageConfig getConfiguration()
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
    public AbstractProcess getSensorDescription(String sensorUID)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<AbstractProcess> getSensorDescriptionHistory(String sensorUID)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AbstractProcess getSensorDescriptionAtTime(String sensorUID, long time)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void store(AbstractProcess process)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void update(AbstractProcess process)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void remove(String sensorUID, long time)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeHistory(String sensorUID)
    {
        // TODO Auto-generated method stub
        
    }
}
