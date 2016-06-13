/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.service;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.module.ModuleConfig;


/**
 * <p>
 * Common configuration options for all clients connecting to remote services
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 9, 2015
 */
public class ClientConfig extends ModuleConfig
{

    @DisplayInfo(label="Connection Timeout", desc="For each reconnection attempt, client will wait for the remote service to respond until this timeout expires (in ms)")
    public int connectTimeout = 1000;
    
    
    @DisplayInfo(label="Reconnection Period", desc="Period at which client will attempt to reconnect when the connection is not available or lost (in ms)")
    public int reconnectPeriod = 2000;
    
    
    @DisplayInfo(label="Reconnection Timeout", desc="In case the connection is not available or lost, client will attempt to reconnect until this timeout expires (in s)."
                     + "If set to 0, the client will never attempt to reconnect")
    public int reconnectTimeout = 0;
}
