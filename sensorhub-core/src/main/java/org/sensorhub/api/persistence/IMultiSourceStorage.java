/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;

import java.util.List;
import net.opengis.sensorml.v20.AbstractProcess;


/**
 * <p>
 * Interface for multi-source storage implementations
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since March 1, 2014
 */
public interface IMultiSourceStorage
{    
    
	/**
     * Retrieves data source description using the specified unique id
     * @param uid unique ID of data source
     * @return SensorML process description
     */
    public AbstractProcess getDataSourceDescription(String uid);
    
    
    /**
     * Retrieves data source description using the specified unique id
     * @param uid unique ID of data source
     * @param startTime lower bound of the time period
     * @param endTime upper bound of the time period
     * @return list of descriptions for the selected data source (with disjoint time validity periods) 
     */
    public List<AbstractProcess> getDataSourceDescriptionHistory(String uid, double startTime, double endTime);
	
	
    /**
     * Retrieves data source description valid at specified time
     * @param uid unique ID of data source
     * @param time
     * @return SensorML process description
     */
    public AbstractProcess getDataSourceDescriptionAtTime(String uid, double time);
    
	
    /**
     * Stores a new data source description into storage.<br/>
     * Validity period must not overlap with existing descriptions
     * @param process SensorML process description to store
     * @throws StorageException 
     */
    public void storeDataSourceDescription(AbstractProcess process) throws StorageException;
    
    
	/**
	 * Updates the data source description in storage.<br/>
	 * Validity period must be exactly the same as the one in storage
	 * @param process SensorML process description to update
	 * @throws StorageException 
	 */
    public void updateDataSourceDescription(AbstractProcess process) throws StorageException;
    

    /**
     * Removes data source description valid at specified time
     * @param uid unique ID of data source
     * @param time 
     */
    public void removeDataSourceDescription(String uid, double time);
    
    
    /**
     * Removes data source sensor description history
     * @param uid unique ID of data source
     * @param startTime lower bound of the time period
     * @param endTime upper bound of the time period
     */
    public void removeDataSourceDescriptionHistory(String uid, double startTime, double endTime);
}
