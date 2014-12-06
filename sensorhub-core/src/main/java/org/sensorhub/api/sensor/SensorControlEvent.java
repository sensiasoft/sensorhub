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

import org.sensorhub.api.common.CommandStatus;


/**
 * <p>
 * Special type of immutable event carrying status data by reference
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 5, 2010
 */
public class SensorControlEvent extends SensorEvent
{
	private static final long serialVersionUID = -1682605821666177558L;
	
	
	/**
	 * Status of the command that triggered this event
	 */
    protected CommandStatus status;
		
	
    /**
     * Default constructor
     * @param time unix time of event generation
     * @param controlInterface source of this event
     * @param status status of command at time the event is generated
     */
	public SensorControlEvent(long time, ISensorControlInterface controlInterface, CommandStatus status)
	{
	    super(time, controlInterface.getParentSensor().getLocalID(), Type.COMMAND_STATUS);
		this.source = controlInterface;
		this.status = status;
	}
	

    public CommandStatus getStatus()
    {
        return status;
    }
}
