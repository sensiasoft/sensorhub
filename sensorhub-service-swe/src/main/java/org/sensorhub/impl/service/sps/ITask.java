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


/**
 * <p>
 * Interface for a SPS task
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 12, 2014
 */
public interface ITask
{

    public abstract String getID();


    public abstract String getUserID();


    public abstract void setUserID(String userID);


    public abstract TaskingRequest getRequest();


    public abstract void setRequest(TaskingRequest request);


    public abstract DateTime getCreationTime();


    public abstract void setCreationTime(DateTime creationTime);


    public abstract DateTime getLatestResponseTime();


    public abstract void setLatestResponseTime(DateTime lastResponseTime);


    public abstract DateTime getExpirationTime();


    public abstract void setExpirationTime(DateTime expirationTime);


    public abstract StatusReport getStatusReport();


    public abstract void setStatusReport(StatusReport status);

}