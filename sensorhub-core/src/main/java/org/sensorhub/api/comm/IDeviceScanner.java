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


/**
 * <p>
 * Interface for device scanning services.<br/>
 * Implementations of this interface are typically used by sensor drivers
 * to discover sensors connected via USB, bluetooth, zeroconf, etc.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 6, 2016
 */
public interface IDeviceScanner
{

    /**
     * Scans for any connected device
     * @param callback callback to which scan results are delivered
     */
    public void startScan(IDeviceScanCallback callback);
    
    
    /**
     * Scans for connected devices with device ID matching the given regex pattern<br/>
     * This will only generate events when the device ID matches the regex pattern
     * @param callback callback to which scan results are delivered
     * @param idRegex Regex pattern for filtering on the device ID
     */
    public void startScan(IDeviceScanCallback callback, String idRegex);
    
    
    /**
     * Stop the current scan
     */
    public void stopScan();
    
    
    /**
     * @return true if a scan is in process, false otherwise
     */
    public boolean isScanning();
    
}
