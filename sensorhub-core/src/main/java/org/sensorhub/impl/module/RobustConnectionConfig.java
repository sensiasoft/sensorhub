/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.module;

import org.sensorhub.api.config.DisplayInfo;


/**
 * <p>
 * Common configuration options for robust connections
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jun 25, 2016
 */
public class RobustConnectionConfig
{

    @DisplayInfo(label="Connection Timeout", desc="For each reconnection attempt, client will wait for the remote side to respond until this timeout expires (in ms)")
    public int connectTimeout = 3000;
    
    
    @DisplayInfo(label="Reconnect Period", desc="Period at which client will attempt to reconnect when the connection is not available or lost (in ms)")
    public int reconnectPeriod = 10000;
    
    
    @DisplayInfo(label="Max Reconnect Attempts", desc="Maximum number of times the client will attempt to reconnect when the connection is not available or lost")
    public int reconnectAttempts = 0;
}
