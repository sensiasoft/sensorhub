/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;


/**
 * <p>
 * Simple data structure to hold status information for a command
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public class CommandStatus
{
	public enum StatusCode {PENDING, ACCEPTED, REJECTED, COMPLETED, FAILED, CANCELLED};
	
	public String id;
	public StatusCode status;
	public String subCode;
	public String message;
	public long updateTime;
	public CommandStatus previousStatus;
	
	
	public CommandStatus()
	{	    
	}
	
	
	public CommandStatus(String id, StatusCode status)
	{
	    this.id = id;
	    this.status = status;
	    this.updateTime = System.currentTimeMillis();
	}
}
