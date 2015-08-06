/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence;

import org.sensorhub.api.persistence.DataFilter;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.IRecordStorageModule;


/**
 * <p>
 * Implementation of purging policy removing records when they reach a 
 * certain age
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jul 26, 2015
 */
public class MaxAgeAutoPurgePolicy implements IStorageAutoPurgePolicy
{
    MaxAgeAutoPurgeConfig config;
    
    
    MaxAgeAutoPurgePolicy(MaxAgeAutoPurgeConfig config)
    {
        this.config = config;
    }
    
    
    @Override
    public int trimStorage(IRecordStorageModule<?> storage)
    {
        int numDeletedRecords = 0;
        
        for (IRecordStoreInfo streamInfo: storage.getRecordStores().values())
        {
            double[] timeRange = storage.getRecordsTimeRange(streamInfo.getName());
            double beginTime = timeRange[0];
            double endTime = timeRange[1];
            
            if (beginTime < endTime - config.maxRecordAge)
            {
                final double[] obsoleteTimeRange = new double[] {beginTime, endTime - config.maxRecordAge};
                
                // remove records
                numDeletedRecords += storage.removeRecords(new DataFilter(streamInfo.getName())
                {
                    public double[] getTimeStampRange()
                    {
                        return obsoleteTimeRange;
                    }                    
                });
                
                // remove data source descriptions
                storage.removeDataSourceDescriptionHistory(obsoleteTimeRange[0], obsoleteTimeRange[1]);
            }
        }
        
        storage.commit();
        return numDeletedRecords;
    }

}
