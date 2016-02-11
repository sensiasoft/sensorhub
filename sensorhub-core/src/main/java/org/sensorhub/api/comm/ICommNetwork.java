/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.comm;

import java.util.Collection;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;


/**
 * <p>
 * Interface used to represent communication networks of different types, such
 * as Ethernet, WiFi, Bluetooth, Bluetooth LE, ZigBee, etc...
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ConfigType> Type of network configuration
 * @since Feb 9, 2016
 */
public interface ICommNetwork<ConfigType extends ModuleConfig> extends IModule<ConfigType>
{
    public enum NetworkType
    {
        IP,
        ETHERNET,
        WIFI,
        CELLULAR,
        BLUETOOTH,
        ZIGBEE,
        VPN,
        UNKNOWN
    }
    
    
    /**
     * @return Name of network interface
     */
    public String getInterfaceName();
    
    
    /**
     * @return Type of network (Ethernet, WiFi, etc.)
     */
    public NetworkType getNetworkType();
    
    
    /**
     * @return Device scanner for this network or null if device discovery is not supported
     */
    public IDeviceScanner getDeviceScanner();
    
    
    /**
     * @return the list of networks available with this module
     */
    public Collection<? extends INetworkInfo> getAvailableNetworks();
    
    
    /**
     * Creates a comm provider working on this network using the provided
     * configuration
     * @param config
     * @return new comm provider instance for connecting to a device
     */
    public ICommProvider<?> newCommProvider(CommConfig config);
}
