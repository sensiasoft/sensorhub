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
 * Base interface for filter objects allowing to get records by criteria
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 12, 2010
 */
public interface IDataFilter
{
    /**
     * Sets the range of time stamps for which data should be retrieved
     * @param start
     * @param stop
     */
    public void setTimeStampRange(long start, long stop);
    
    
    /**
     * @return the time stamp range to filter on
     */
    public long[] getTimeStampRange();
    
    
    /**
     * Sets the producer ID to filter on
     * @param localID
     */
    public void setProducerID(String localID);
    
    
    /**
     * @return the producer ID to filter on
     */
    public String getProducerID();
}
