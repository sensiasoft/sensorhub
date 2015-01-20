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

package org.sensorhub.impl.service.wsn;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import org.apache.muse.ws.addressing.EndpointReference;
import org.apache.muse.ws.notification.Filter;


public class SubscriptionInfo implements Serializable
{
	private static final long serialVersionUID = 3112001090965898367L;
	
	protected String uid;
	protected String userID;
	protected EndpointReference producer;
	protected EndpointReference consumer;
	protected EndpointReference endpoint;
	protected Filter filter;
	protected Date terminationTime;
	protected int numberOfTries = 3;
	
	
	public SubscriptionInfo()
	{
		this.uid = UUID.randomUUID().toString();
	}
	
	
	public SubscriptionInfo(String uid)
	{
		this();
		this.uid = uid;
	}
	
	
	public String getUid()
	{
		return uid;
	}


	public void setUid(String uid)
	{
		this.uid = uid;
	}


	public String getUserID()
	{
		return userID;
	}


	public void setUserID(String userID)
	{
		this.userID = userID;
	}


	public EndpointReference getConsumer()
	{
		return this.consumer;
	}


	public void setConsumer(EndpointReference consumer)
	{
		this.consumer = consumer;
	}
	
	
	public EndpointReference getProducer()
	{
		return this.producer;
	}


	public void setProducer(EndpointReference producer)
	{
		this.producer = producer;
	}


	public EndpointReference getEndpoint()
	{
		return endpoint;
	}


	public void setEndpoint(EndpointReference endpoint)
	{
		this.endpoint = endpoint;
	}


	public Filter getFilter()
	{
		return this.filter;
	}


	public void setFilter(Filter filter)
	{
		this.filter = filter;
	}


	public Date getTerminationTime()
	{
		return terminationTime;
	}


	public void setTerminationTime(Date terminationTime)
	{
		this.terminationTime = terminationTime;
	}


	public int getNumberOfTries()
	{
		return numberOfTries;
	}


	public void setNumberOfTries(int numberOfTries)
	{
		this.numberOfTries = numberOfTries;
	}
}
