/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.sensor;

import java.util.Collection;
import org.sensorhub.api.module.IModuleManager;
import org.sensorhub.api.module.IModuleProvider;


/**
 * <p>
 * Management interface for sensors connected to the system
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public interface ISensorManager extends IModuleManager<ISensorModule<?>>
{ 
    
    /**
     * Helper method to find a sensor by its global UID (instead of its localID)
     * @param uid global unique identifier of sensor (= SensorML UID)
     * @return sensor module instance
     */
    public ISensorModule<?> findSensor(String uid);
    
    
	/**
     * Helper method to get the list of connected sensors only
     * @return the list of sensors actually connected to the system
     */
    public Collection<ISensorModule<?>> getConnectedSensors();
    
    
    /**
	 * Installs a driver package (jar file) from the specified URL
	 * @param driverPackageURL URL of jar containing implementation of new driver
     * @param replace if true, an older version of the same driver will be replaced
	 * @return automatically assigned driver ID
	 */
	public String installDriver(String driverPackageURL, boolean replace);
	
	
	/**
	 * Uninstalls the driver with the specified ID
	 * @param driverID
	 */
	public void uninstallDriver(String driverID);
	
	
	/**
	 * @return the list of all sensor drivers installed on the system
	 */
	public Collection<IModuleProvider> getInstalledSensorDrivers();	

}
