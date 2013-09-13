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
     * @return
     */
    public abstract List<StorageIndexDescriptor> getAllFieldIndexes();

}