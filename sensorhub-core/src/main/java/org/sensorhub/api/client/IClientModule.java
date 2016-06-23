/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.client;

import org.sensorhub.api.module.IModule;


/**
 * <p>
 * Common base for client modules connecting to remote services
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ConfigType> 
 * @since Apr 1, 2016
 */
public interface IClientModule<ConfigType extends ClientConfig> extends IModule<ConfigType>
{
    
    /**
     * Returns the client connection status.<br/>
     * @return true if client is actually connected and can communicate with
     * the remote service
     */
    public boolean isConnected();
}
