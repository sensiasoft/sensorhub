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


/**
 * <p>
 * Base class for all data storage keys used in SensorHub.
 * Keys are used to store and retrieve data records from a storage.
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 6, 2010
 */
public class DataKey
{
    /**
     * Local ID of record to uniquely identify it in storage
     */
    public long recordID;
    
    
    /**
     * ID of data producer (i.e. the entity that produced the values in the data block)
     */
    public String producerID;
    
    
    /**
     * Time stamp of record
     */
    public long timeStamp;
    
    
    /**
     * Default constructor providing basic indexing metadata
     * @param producerID
     * @param timeStamp
     */
    public DataKey(String producerID, long timeStamp)
    {
        this.producerID = producerID;
        this.timeStamp = timeStamp;
    }
}
