/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;


/**
 * <p>
 * Immutable object for storing ranges of values of any types
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <DataType> 
 * @since Nov 12, 2010
 */
public class ValueRange<DataType>
{
    protected DataType low;
    protected DataType high;
    
    
    public ValueRange(DataType low, DataType high)
    {
        this.low = low;
        this.high = high;
    }


    /**
     * @return low value
     */
    public DataType getLow()
    {
        return low;
    }


    /**
     * @return high value
     */
    public DataType getHigh()
    {
        return high;
    }
}
