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

import org.sensorhub.api.module.ModuleConfig;


public class SensorStorageHelperConfig extends ModuleConfig
{
    private static final long serialVersionUID = -7523211661433806575L;

    
    /**
     * Local ID of storage to store the data into
     */
    public String storageID;
    
    
    /**
     * Local ID of sensor which data needs to be stored
     */
    public String sensorID;
    
    
    /**
     * Names of sensor outputs to save to storage
     */
    public String[] selectedOutputs;
    
    
    public SensorStorageHelperConfig()
    {
        // set default associated implementation
        this.moduleClass = SensorStorageHelper.class.getCanonicalName();
    }
}
