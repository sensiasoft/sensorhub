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

import org.sensorhub.api.config.DisplayInfo;


/**
 * <p>
 * Configuration for automatic storage purge
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jul 25, 2015
 */
public class MaxAgeAutoPurgeConfig extends StorageAutoPurgeConfig
{
    
    @DisplayInfo(label="Max Record Age", desc="Maximum age of data to be kept in storage (in seconds)")
    public double maxRecordAge = 7*24*3600;
    
    
    @Override
    public IStorageAutoPurgePolicy getPolicy()
    {
        return new MaxAgeAutoPurgePolicy(this);
    }
}
