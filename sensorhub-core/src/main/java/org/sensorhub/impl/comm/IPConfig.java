/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.comm;

import org.sensorhub.api.comm.CommConfig;
import org.sensorhub.api.comm.ICommNetwork.NetworkType;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.AddressType;
import org.sensorhub.api.config.DisplayInfo.FieldType;
import org.sensorhub.api.config.DisplayInfo.FieldType.Type;


/**
 * <p>
 * Driver configuration options for IP network protocols
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 5, 2016
 */
public abstract class IPConfig extends CommConfig
{	
    public final static String AUTO_NETIF = "AUTO";
    
    
    @DisplayInfo(desc="IP or DNS name of remote host")
    @FieldType(Type.REMOTE_ADDRESS)
    @AddressType(NetworkType.IP)
    public String remoteHost; 
    
    
    @DisplayInfo(desc="IP of local network interface to bind to or 'AUTO' to select it automatically")
    @FieldType(Type.LOCAL_ADDRESS)
    @AddressType(NetworkType.IP)
    public String localAddress = AUTO_NETIF;
}
