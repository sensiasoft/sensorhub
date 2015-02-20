/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.net;

import org.sensorhub.api.module.IModuleManager;


/**
 * <p>
 * Management interface for communication devices (Ethernet, GSM, Satcom, etc.)
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public interface ICommManager extends IModuleManager<ICommInterface>
{
    /**
     * Installs a communication driver package (jar file) from the specified URL
     * @param driverPackageURL URL of jar containing implementation of new driver
     * @param replace 
     * @return automatically assigned driver ID
     */
    public String installDriver(String driverPackageURL, boolean replace);
    
    
    /**
     * Uninstalls the driver with the specified ID
     * @param driverID
     */
    public void uninstallDriver(String driverID);
}
