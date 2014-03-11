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
 
 Please Contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.persistence.ISensorDescriptionStorage;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.persistence.StorageException;
import org.vast.sensorML.SMLProcess;


public class InMemorySensorStorage implements ISensorDescriptionStorage<StorageConfig>
{
    StorageConfig config;
    Map<String, List<SMLProcess>> smlTable;


    @Override
    public void open() throws StorageException
    {
        smlTable = new HashMap<String, List<SMLProcess>>();
    }


    @Override
    public void close() throws StorageException
    {
    }


    @Override
    public void cleanup() throws StorageException
    {
        smlTable.clear();
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
    public void sync(IStorageModule<?> storage)
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
    }


    @Override
    public void loadState(IModuleStateLoader loader) throws SensorHubException
    {
    }


    @Override
    public void registerListener(IEventListener listener)
    {
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
    }


    @Override
    public SMLProcess getSensorDescription(String sensorUID)
    {
        return smlTable.get(sensorUID).get(0);
    }


    @Override
    public List<SMLProcess> getSensorDescriptionHistory(String sensorUID)
    {
        return smlTable.get(sensorUID);
    }


    @Override
    public SMLProcess getSensorDescriptionAtTime(String sensorUID, long time)
    {
        for (SMLProcess process: smlTable.get(sensorUID))
        {
            long begin = process.getMetadata().getValidityBegin().getTime();
            long end = process.getMetadata().getValidityEnd().getTime();
            if (time > begin && time < end)
                return process;
        }
        
        return null;
    }


    @Override
    public void store(SMLProcess process)
    {
        List<SMLProcess> processList = smlTable.get(process.getIdentifier());
        if (processList == null)
        {
            processList = new ArrayList<SMLProcess>();
            smlTable.put(process.getIdentifier(), processList);
        }
        
        processList.add(process);
    }


    @Override
    public void update(SMLProcess process)
    {
        List<SMLProcess> processList = smlTable.get(process.getIdentifier());
        if (processList == null)
            processList = new ArrayList<SMLProcess>();
        
        processList.add(process);
    }


    @Override
    public void remove(String sensorUID, long time)
    {
        ListIterator<SMLProcess> it = smlTable.get(sensorUID).listIterator();
        while (it.hasNext())
        {
            SMLProcess p = it.next();
            long begin = p.getMetadata().getValidityBegin().getTime();
            long end = p.getMetadata().getValidityEnd().getTime();
            if (time > begin && time < end)
            {
                it.remove();
                break;
            }
        }
    }


    @Override
    public void removeHistory(String sensorUID)
    {
        smlTable.remove(sensorUID);
    }

}
