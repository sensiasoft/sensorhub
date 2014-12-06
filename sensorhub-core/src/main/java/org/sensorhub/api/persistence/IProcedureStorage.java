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
import net.opengis.sensorml.v20.AbstractProcess;


/**
 * <p>
 * Base interface for all sensor description storage implementations
 * </p>
 *
 * <p>Copyright (c) 2014</p>
 * @author Alexandre Robin
 * @param <ConfigType> Configuration class
 * @since March 1, 2014
 */
public interface IProcedureStorage<ConfigType extends StorageConfig> extends IStorageModule<ConfigType>
{    
    
	/**
     * Retrieves sensor description using the specified unique id
     * @param sensorUID
     * @return SensorML process description
     */
    public AbstractProcess getSensorDescription(String sensorUID);
    
    
    /**
     * Retrieves sensor description using the specified unique id
     * @param sensorUID
     * @return list of descriptions for the selected sensor (with disjoint time validity periods) 
     */
    public List<AbstractProcess> getSensorDescriptionHistory(String sensorUID);
	
	
    /**
     * Retrieves sensor description valid at specified time
     * @param sensorUID
     * @param time
     * @return SensorML process description
     */
    public AbstractProcess getSensorDescriptionAtTime(String sensorUID, long time);
    
	
    /**
     * Store a new sensor description into storage
     * Validity period must not overlap with existing descriptions
     * @param process SensorML process description to store
     */
    public void store(AbstractProcess process);
    
    
	/**
	 * Update the sensor description in storage
	 * Validity period must be exactly the same as the one in storage
	 * @param process SensorML process description to update
	 */
    public void update(AbstractProcess process);
    

    /**
     * Removes sensor description valid at specified time
     * @param sensorUID
     * @param time 
     */
    public void remove(String sensorUID, long time);
    
    
    /**
     * Removes entire sensor description history
     * @param sensorUID
     */
    public void removeHistory(String sensorUID);
}
