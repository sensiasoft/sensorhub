/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.processing;

import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.module.IModule;


/**
 * <p>
 * Base interface for all SensorHub data processing modules
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public interface IProcess extends IModule<ProcessConfig>
{
    /**
     * @return inputs descriptor
     */
    public DataComponent getInputList();


    /**
     * @return outputs descriptor
     */
    public DataComponent getOutputList();
    
    
    /**
     * @return parameters descriptor
     */
    public DataComponent getParameterList();
}
