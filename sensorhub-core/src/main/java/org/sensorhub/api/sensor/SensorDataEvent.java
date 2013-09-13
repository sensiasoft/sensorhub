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

import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;


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
	 * @param desc
	 * @param records 
	 */
	public SensorDataEvent(String sensorId, DataComponent desc, DataBlock... records)
	{
		super(sensorId, Type.NEW_DATA_AVAILABLE);
		this.recordDescription = desc;
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
}
