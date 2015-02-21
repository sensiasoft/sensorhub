/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sps;

import org.vast.ows.sps.StatusReport;
import org.vast.ows.sps.TaskingRequest;
import org.vast.util.DateTime;


public class Task implements ITask
{
	protected TaskingRequest request;
	protected StatusReport statusReport;	
	protected String userID;
	protected DateTime creationTime;
	protected DateTime latestResponseTime;
	protected DateTime expirationTime;
	
	
	public Task()
	{
		this.statusReport = new StatusReport();
	}
	
	
	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#getID()
     */
	@Override
    public String getID()
	{
		return statusReport.getTaskID();
	}
	
	
	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#getUserID()
     */
	@Override
    public String getUserID()
	{
		return userID;
	}


	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#setUserID(java.lang.String)
     */
	@Override
    public void setUserID(String userID)
	{
		this.userID = userID;
	}
	
	
	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#getRequest()
     */
	@Override
    public TaskingRequest getRequest()
	{
		return request;
	}


	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#setRequest(org.vast.ows.sps.TaskingRequest)
     */
	@Override
    public void setRequest(TaskingRequest request)
	{
		this.request = request;
	}


	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#getCreationTime()
     */
	@Override
    public DateTime getCreationTime()
	{
		return creationTime;
	}


	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#setCreationTime(org.vast.util.DateTime)
     */
	@Override
    public void setCreationTime(DateTime creationTime)
	{
		this.creationTime = creationTime;
	}


	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#getLatestResponseTime()
     */
	@Override
    public DateTime getLatestResponseTime()
	{
		return latestResponseTime;
	}


	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#setLatestResponseTime(org.vast.util.DateTime)
     */
	@Override
    public void setLatestResponseTime(DateTime lastResponseTime)
	{
		this.latestResponseTime = lastResponseTime;
	}


	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#getExpirationTime()
     */
	@Override
    public DateTime getExpirationTime()
	{
		return expirationTime;
	}


	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#setExpirationTime(org.vast.util.DateTime)
     */
	@Override
    public void setExpirationTime(DateTime expirationTime)
	{
		this.expirationTime = expirationTime;
	}


	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#getStatusReport()
     */
	@Override
    public StatusReport getStatusReport()
	{
		return statusReport;
	}


	/* (non-Javadoc)
     * @see org.sensorhub.impl.service.sps.ISpsTask#setStatusReport(org.vast.ows.sps.StatusReport)
     */
	@Override
    public void setStatusReport(StatusReport status)
	{
		this.statusReport = status;
	}
}
