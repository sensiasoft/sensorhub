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

package org.sensorhub.api.persistence;

import java.util.List;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleManager;


/**
 * <p>
 * Management interface for persistent storage modules
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 5, 2010
 */
public interface IPersistenceManager extends IModuleManager<IStorageModule<?>>
{  
    
    /**
     * Finds storage modules where the specified sensor data is archived
     * @param sensorID Local ID of sensor to find existing storage for
     * @return list of storage instances or empty list if none are found
     * @throws SensorHubException
     */
    public List<? extends IBasicStorage<?>> findStorageForSensor(String sensorID) throws SensorHubException;
    
    
    /**
     * Gets the default config for the specified storage type
     * @param storageClass Concrete storage class
     * @return default config class, pre-filled with proper information
     * @throws SensorHubException
     */
    public StorageConfig getDefaultStorageConfig(Class<?> storageClass) throws SensorHubException;
    
}
