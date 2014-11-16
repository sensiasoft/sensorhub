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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import net.opengis.gml.v32.AbstractTimeGeometricPrimitive;
import net.opengis.gml.v32.TimeInstant;
import net.opengis.gml.v32.TimePeriod;
import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.persistence.ISensorDescriptionStorage;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.api.persistence.StorageException;


public class InMemorySensorStorage implements ISensorDescriptionStorage<StorageConfig>
{
    StorageConfig config;
    Map<String, List<AbstractProcess>> smlTable;


    @Override
    public void open() throws StorageException
    {
        smlTable = new HashMap<String, List<AbstractProcess>>();
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
    public AbstractProcess getSensorDescription(String sensorUID)
    {
        return smlTable.get(sensorUID).get(0);
    }


    @Override
    public List<AbstractProcess> getSensorDescriptionHistory(String sensorUID)
    {
        return smlTable.get(sensorUID);
    }


    @Override
    public AbstractProcess getSensorDescriptionAtTime(String sensorUID, long time)
    {
        for (AbstractProcess process: smlTable.get(sensorUID))
        {
            if (isTimeMatch(process, time))
                return process;
        }
        
        return null;
    }


    @Override
    public void store(AbstractProcess process)
    {
        List<AbstractProcess> processList = smlTable.get(process.getIdentifier());
        if (processList == null)
        {
            processList = new ArrayList<AbstractProcess>();
            smlTable.put(process.getIdentifier().getValue(), processList);
        }
        
        processList.add(process);
    }


    @Override
    public void update(AbstractProcess process)
    {
        List<AbstractProcess> processList = smlTable.get(process.getIdentifier());
        if (processList == null)
            processList = new ArrayList<AbstractProcess>();
        
        processList.add(process);
    }


    @Override
    public void remove(String sensorUID, long time)
    {
        ListIterator<AbstractProcess> it = smlTable.get(sensorUID).listIterator();
        while (it.hasNext())
        {
            AbstractProcess p = it.next();
            if (isTimeMatch(p, time))
            {
                it.remove();
                break;
            }
        }
    }
    
    
    protected boolean isTimeMatch(AbstractProcess process, long time)
    {
        for (AbstractTimeGeometricPrimitive validTime: process.getValidTimeList())
        {
            if (validTime instanceof TimePeriod)
            {
                long begin = (long) ((TimePeriod)validTime).getBeginPosition().getDateTimeValue().getAsDouble() * 1000;
                long end = (long) ((TimePeriod)validTime).getEndPosition().getDateTimeValue().getAsDouble() * 1000;
                if (time > begin && time < end)
                    return true;
            }
            else if (validTime instanceof TimeInstant)
            {
                long docTime = (long) ((TimeInstant)validTime).getTimePosition().getDateTimeValue().getAsDouble() * 1000;
                if (time == docTime)
                    return true;
            }
        }
        
        return false;
    }


    @Override
    public void removeHistory(String sensorUID)
    {
        smlTable.remove(sensorUID);
    }

}
