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

import org.sensorhub.api.data.DataEvent;
import net.opengis.swe.v20.DataBlock;


/**
 * <p>
 * Type of event generated when new data is available from sensors.
 * It is immutable and carries sensor data by reference
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public class SensorDataEvent extends DataEvent
{    
	
	/**
	 * Constructor from list of records with their descriptor
	 * @param timeStamp time of event generation (unix time in milliseconds, base 1970)
     * @param dataInterface sensor output interface that produced the associated data
	 * @param records arrays of records that triggered this notification
	 */
	public SensorDataEvent(long timeStamp, ISensorDataInterface dataInterface, DataBlock ... records)
	{
		super(timeStamp, dataInterface, records);
	}


    @Override
    public ISensorDataInterface getSource()
    {
        return (ISensorDataInterface)this.source;
    }
    
    
    public String getSensorID()
    {
        return getSource().getParentModule().getLocalID();
    }
}
