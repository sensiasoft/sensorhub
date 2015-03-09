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


/**
 * <p>
 * TODO CustomPanelConfig type description
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 13, 2015
 */
public class CustomPanelConfig
{
    
    /**
     * Type of config class for which a custom form must be generated
     */
    public String configClass;
    
    
    /**
     * Fully qualified name of class implementing IModuleConfigFormBuilder
     */
    public String builderClass;
    
    
    public CustomPanelConfig(String configClass, String builderClass)
    {
        this.configClass = configClass;
        this.builderClass = builderClass;
    }
}
