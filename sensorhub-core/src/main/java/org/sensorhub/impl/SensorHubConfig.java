/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl;

import org.sensorhub.api.config.IGlobalConfig;


/**
 * <p>
 * Configuration class containing bootstrap configuration options
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 20, 2013
 */
public class SensorHubConfig implements IGlobalConfig
{
    private String moduleConfigPath;
    private String baseStoragePath;
    
    
    public SensorHubConfig()
    {        
    }
    
    
    public SensorHubConfig(String moduleConfigPath, String baseStoragePath)
    {
        this.moduleConfigPath = moduleConfigPath;
        this.baseStoragePath = baseStoragePath;
    }
    
    
    @Override
    public String getModuleConfigPath()
    {
        return moduleConfigPath;
    }

    
    @Override
    public String getBaseStoragePath()
    {
        return baseStoragePath;
    }
    

    @Override
    public String getProperty(String property)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
