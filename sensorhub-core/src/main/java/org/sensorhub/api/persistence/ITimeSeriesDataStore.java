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


/**
 * <p>
 * This interface adds time tagging support to a record storage
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <FilterType> 
 * @since Nov 27, 2014
 */
public interface ITimeSeriesDataStore<FilterType extends IDataFilter> extends IRecordDataStore<DataKey, FilterType>
{

    /**
     * Get time range of all data contained in this storage.
     * @return array of length 2 in the form [minTime maxTime] or [NaN NaN] is data store is empty
     */
    public double[] getDataTimeRange();
    
}
