/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
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
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
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
	 * Array of available data records (by reference)
	 * Data is NOT automatically cleared from sensor buffer and may still
	 * be available through the polling API.
	 */
	protected DataBlock[] records;
	
	
	/**
	 * Constructor from list of records with their descriptor
	 * @param dataInterface 
	 * @param timeStamp 
	 * @param desc
	 * @param records 
	 */
	public SensorDataEvent(ISensorDataInterface dataInterface, long timeStamp, DataComponent desc, DataBlock... records)
	{
		super(dataInterface.getSensorInterface().getLocalID(), Type.NEW_DATA_AVAILABLE);
		this.timeStamp = timeStamp;
		this.recordDescription = desc;
		this.records = records;
		this.source = dataInterface;
	}


    public DataComponent getRecordDescription()
    {
        return recordDescription;
    }


    public DataBlock[] getRecords()
    {
        return records;
    }
}
