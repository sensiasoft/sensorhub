/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.sensor;

import java.util.List;
import org.sensorhub.api.module.IModuleManager;
import org.sensorhub.api.module.IModuleProvider;


/**
 * <p>
 * Management interface for sensors connected to the system
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 5, 2010
 */
public interface ISensorManager extends IModuleManager<ISensorModule<?>>
{ 
    
    /**
     * Helper method to find a sensor by its global UID
     * @param uid
     * @return
     */
    public ISensorModule<?> findSensor(String uid);
    
    
	/**
     * Helper method to get the list of connected sensors only
     * @return the list of sensors actually connected to the system
     */
    public List<ISensorModule<?>> getConnectedSensors();
    
    
    /**
	 * Installs a driver package (jar file) from the specified URL
	 * @param driverPackageuURL URL of jar containing implementation of new driver
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
	public List<IModuleProvider> getInstalledSensorDrivers();	

}
