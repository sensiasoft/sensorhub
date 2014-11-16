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
     * @param controlInterface Source of this event
     * @param status Status of command at time the event is generated
     */
	public SensorControlEvent(ISensorControlInterface controlInterface, CommandStatus status)
	{
	    super(controlInterface.getParentSensor().getLocalID(), Type.COMMAND_STATUS);
		this.status = status;
		this.source = controlInterface;
	}
	

    public CommandStatus getStatus()
    {
        return status;
    }
}
