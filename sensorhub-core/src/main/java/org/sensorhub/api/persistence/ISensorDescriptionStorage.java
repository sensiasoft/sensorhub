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

package org.sensorhub.api.persistence;

import java.util.List;
import org.sensorhub.api.common.IEventProducer;
import org.vast.sensorML.SMLProcess;


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
public interface ISensorDescriptionStorage<ConfigType extends StorageConfig> extends IStorageModule<ConfigType>, IEventProducer
{    
    
	/**
     * Retrieves sensor description using the specified unique id
     * @param sensorUID
     * @return SensorML process description
     */
    public SMLProcess getSensorDescription(String sensorUID);
    
    
    /**
     * Retrieves sensor description using the specified unique id
     * @param sensorUID
     * @return list of descriptions for the selected sensor (with disjoint time validity periods) 
     */
    public List<SMLProcess> getSensorDescriptionHistory(String sensorUID);
	
	
    /**
     * Retrieves sensor description valid at specified time
     * @param sensorUID
     * @param time
     * @return SensorML process description
     */
    public SMLProcess getSensorDescriptionAtTime(String sensorUID, long time);
    
	
    /**
     * Store a new sensor description into storage
     * Validity period must not overlap with existing descriptions
     * @param process SensorML process description to store
     */
    public void store(SMLProcess process);
    
    
	/**
	 * Update the sensor description in storage
	 * Validity period must be exactly the same as the one in storage
	 * @param process SensorML process description to update
	 */
    public void update(SMLProcess process);
    

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
