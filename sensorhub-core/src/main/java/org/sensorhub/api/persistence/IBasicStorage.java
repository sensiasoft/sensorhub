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
import java.util.Map;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


/**
 * <p>
 * Storage for one or more data streams coming from a single source.
 * (for instance, the source can be a sensor, a service or a process)
 * 
 * This also supports storing lineage information if a description of the
 * data source is provided. 
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ConfigType> Type of storage configuration
 * @since Nov 27, 2014
 */
public interface IBasicStorage<ConfigType extends StorageConfig> extends IStorageModule<ConfigType>
{
    
    /**
     * Retrieves latest data source description (i.e. most recent version)
     * @return SensorML process description
     */
    public AbstractProcess getLatestDataSourceDescription();
    
    
    /**
     * Retrieves whole history of data source description
     * @return list of SensorML process descriptions (with disjoint time validity periods) 
     */
    public List<AbstractProcess> getDataSourceDescriptionHistory();
    
    
    /**
     * Retrieves data source description valid at specified time
     * @param time
     * @return SensorML process description
     */
    public AbstractProcess getDataSourceDescriptionAtTime(double time);
    
    
    /**
     * Stores a new data source description into storage.
     * Validity period must not overlap with existing descriptions
     * @param process SensorML process description to store
     * @throws StorageException if new description cannot be stored
     */
    public void storeDataSourceDescription(AbstractProcess process) throws StorageException;
    
    
    /**
     * Update the data source description in storage.
     * Validity period must be exactly the same as one in storage
     * @param process SensorML process description to update
     * @throws StorageException if validity period doesn't match any already in storage or new description cannot be stored
     */
    public void updateDataSourceDescription(AbstractProcess process) throws StorageException;
    

    /**
     * Removes data source description valid at specified time
     * @param time any time falling within the validity period of the description version to remove 
     */
    public void removeDataSourceDescription(double time);
    
    
    /**
     * Removes entire data source description history
     */
    public void removeDataSourceDescriptionHistory();
    
    
    /**
     * Gets the list of underlying data stores
     * @return map of name to data store instance
     */
    public Map<String, ? extends ITimeSeriesDataStore<IDataFilter>> getDataStores();
    
    
    /**
     * Adds a new data store for the specified record structure
     * @param name name of record stream (should match output name of the data source)
     * @param recordStructure SWE data component describing the record structure
     * @param recommendedEncoding recommended encoding for this record type
     * @return newly created data store
     * @throws StorageException if new data store cannot be created
     */
    public ITimeSeriesDataStore<IDataFilter> addNewDataStore(String name,
            DataComponent recordStructure, DataEncoding recommendedEncoding) throws StorageException;

}
