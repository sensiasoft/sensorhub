/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.persistence.SensorStorageHelper;
import org.vast.ows.sos.ISOSDataConsumer;


/**
 * <p>
 * Wrapper data consumer for updating both sensor and storage appropriately
 * </p>
 *
 * @author Alex Robin>
 * @since Feb 13, 2015
 */
public class SensorWithStorageConsumer extends SensorDataConsumer implements ISOSDataConsumer
{
    IBasicStorage<?> storage;
    
    
    public SensorWithStorageConsumer(SensorConsumerConfig config) throws SensorHubException
    {
        super(config);
        ModuleRegistry moduleReg = SensorHub.getInstance().getModuleRegistry();
        this.storage = (IBasicStorage<?>)moduleReg.getModuleById(config.storageID);
        
        // reassign current sensor description
        this.sensor.setSensorDescription(storage.getLatestDataSourceDescription());
    }


    @Override
    public String newResultTemplate(DataComponent component, DataEncoding encoding) throws Exception
    {
        String templateID = sensor.newResultTemplate(component, encoding);
        
        if (!storage.getDataStores().containsKey(templateID))
        {
            storage.addNewDataStore(templateID, component, encoding);
            
            // look for storage helper
            ModuleRegistry moduleReg = SensorHub.getInstance().getModuleRegistry();
            for (IModule<?> module: moduleReg.getLoadedModules())
            {
                if (module instanceof SensorStorageHelper)
                {
                    SensorStorageHelper listener = (SensorStorageHelper)module;
                                        
                    if (listener.getConfiguration().sensorID.equals(sensor.getLocalID()))
                    {
                        // register for new events
                        sensor.getAllOutputs().get(templateID).registerListener(listener);
                    }
                }
            }
        }
        
        return templateID;
    }
}
