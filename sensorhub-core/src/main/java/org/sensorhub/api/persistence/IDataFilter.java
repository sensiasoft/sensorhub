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
