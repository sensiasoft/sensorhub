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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Type of event generated when new data is avaible from sensors.
 * It is immutable and carries sensor data by reference
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public class SensorDataEvent extends SensorEvent
{
	private static final long serialVersionUID = 2124599187504793797L;
    
	
	/**
	 * Description of data records contained in this event (by reference) 
	 */
	protected DataComponent recordDescription;
	
	
	/**
	 * New data that triggered this event.
	 * Multiple records can be associated to a single event because for performance
	 * reasonsn with high rate sensors, it is often not practical to generate an
	 * event for every single record of measurements.
	 */
	protected DataBlock[] records;
	
	
	/**
	 * Constructor from list of records with their descriptor
	 * @param timeStamp time of event generation (julian time, base 1970)
     * @param dataInterface sensor output interface that produced the associated data
	 * @param records arrays of records that triggered this notification
	 */
	public SensorDataEvent(double timeStamp, ISensorDataInterface dataInterface, DataBlock ... records)
	{
		super((long)(timeStamp*1000), dataInterface.getParentSensor().getLocalID(), Type.NEW_DATA_AVAILABLE);
		this.source = dataInterface;
		this.recordDescription = dataInterface.getRecordDescription();
		this.records = records;
	}


    public DataComponent getRecordDescription()
    {
        return recordDescription;
    }


    public DataBlock[] getRecords()
    {
        return records;
    }


    @Override
    public ISensorDataInterface getSource()
    {
        return (ISensorDataInterface)this.source;
    }
}
