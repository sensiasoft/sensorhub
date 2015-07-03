/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.module.ModuleConfig;


public class AdminUIConfig extends ModuleConfig
{
    
    public String widgetSet;
    
    
    public List<CustomUIConfig> customPanels = new ArrayList<CustomUIConfig>();
    
    
    public List<CustomUIConfig> customForms = new ArrayList<CustomUIConfig>();
    
    
    public AdminUIConfig()
    {
        this.name = "Admin UI";
    }

}
