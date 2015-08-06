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
import org.sensorhub.api.persistence.StorageConfig;


public class StreamStorageConfig extends StorageConfig
{
    
    @DisplayInfo(label="Storage Config", desc="Configuration of underlying storage")
    public StorageConfig storageConfig;
    
    
    @DisplayInfo(label="Data Source ID", desc="Local ID of streaming data source which data will be store.")
    public String dataSourceID;
    
    
    @DisplayInfo(label="Selected Outputs", desc="Names of data source outputs to save to storage")
    public String[] selectedOutputs;
    
    
    @DisplayInfo(label="Automatic Purge Policy", desc="Policy for automatically purging stored data")
    public StorageAutoPurgeConfig autoPurgeConfig;
    

    @DisplayInfo(desc="Minimum period between database commits (in ms)")
    public int minCommitPeriod = 10000;
    
    
    public StreamStorageConfig()
    {
        // set default associated implementation
        this.moduleClass = GenericStreamStorage.class.getCanonicalName();
    }
}
