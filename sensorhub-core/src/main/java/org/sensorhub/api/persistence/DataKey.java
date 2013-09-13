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
