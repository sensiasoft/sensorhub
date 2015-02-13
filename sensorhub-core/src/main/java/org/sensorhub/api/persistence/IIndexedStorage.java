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
import org.sensorhub.api.persistence.StorageIndexDescriptor.IndexType;


public interface IIndexedStorage
{

    /**
     * Adds index of specified type on field identified by the given path	
     * The specified field can be scalar or complex but must be compatible with the index type
     * @param fieldPath
     * @param type
     * @return index ID
     */
    public abstract StorageIndexDescriptor addFieldIndex(String fieldPath, IndexType type);


    /**
     * Removes the index with the specified ID
     * @param indexId
     */
    public abstract void removeFieldIndex(String indexId);


    /**
     * Retrieve list of all index added to this storage
     * @return list of index descriptors
     */
    public abstract List<StorageIndexDescriptor> getAllFieldIndexes();

}