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
        BLUETOOTH_LE,
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
     * Checks if this network is compatible with given type.
     * @param type
     * @return true if this network supports the given type
     */
    public boolean isOfType(NetworkType type);
    
    
    /**
     * @return Device scanner for this network or null if device discovery is not supported
     */
    public IDeviceScanner getDeviceScanner();
    

    //public void powerOn();
    //public void powerOff();
    
    
    /*
     * Check if network is available (e.g. connected to an access point, etc.)
     * @return true if network is available for communications
     */
    //public boolean isConnected();
    
    
    /*
     * Check if network can route packets to the public Internet
     * @return true if network is connected to Internet
     */
    //public boolean isConnectedToInternet();
    
    
    /*
     * Checks if address is reachable through this network
     * @param address
     * @return
     */
    //public boolean isReachable(String address);
    
    
    /*
     * Registers a listener that get notified when network availability changes
     */
    //public void registerConnectivityListener(IEventListener listener);
    
    
    /**
     * @return the list of networks available with this module
     * -> move to INetworkManager
     */
    public Collection<? extends INetworkInfo> getAvailableNetworks();
}
