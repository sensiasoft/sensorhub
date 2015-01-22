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

package org.sensorhub.impl.service.sps;

import javax.xml.namespace.QName;
import org.sensorhub.impl.service.wsn.ISubscriptionDB;
import org.sensorhub.impl.service.wsn.NotificationSystem;
import org.vast.ows.sps.SPSCommonWriterV20;
import org.vast.ows.sps.SPSUtils;
import org.vast.ows.sps.StatusReport;
import org.vast.xml.DOMHelper;
import org.vast.xml.XMLWriterException;
import org.w3c.dom.Element;


/**
 * Helper class to send notification messages to client
 * subscribed to the given topic channels of SPS 2.0.
 * 
 * @author Alexandre Robin <alexandre.robin@spotimage.fr>
 */
public class SPSNotificationSystem extends NotificationSystem
{
	public static String SPS_NS_URI = SPSUtils.getNamespaceURI(SPSUtils.SPS, "2.0");
	public static QName TASK_SUBMISSION_TOPIC = new QName(SPS_NS_URI, "TaskSubmission", "sps");
	public static QName TASK_COMPLETION_TOPIC = new QName(SPS_NS_URI, "TaskCompletion", "sps");
	public static QName TASK_FAILURE_TOPIC = new QName(SPS_NS_URI, "TaskFailure", "sps");
	public static QName TASK_CANCELLATION_TOPIC = new QName(SPS_NS_URI, "TaskCancellation", "sps");
	public static QName TASK_UPDATE_TOPIC = new QName(SPS_NS_URI, "TaskUpdate", "sps");
	public static QName DATA_PUBLICATION_TOPIC = new QName(SPS_NS_URI, "DataPublication", "sps");
	public static QName TASK_RESERVATION_TOPIC = new QName(SPS_NS_URI, "TaskReservation", "sps");
	public static QName TASK_CONFIRMATION_TOPIC = new QName(SPS_NS_URI, "TaskConfirmation", "sps");
	public static QName RESERVATION_EXPIRATION_TOPIC = new QName(SPS_NS_URI, "ReservationExpiration", "sps");
	public static QName REQUEST_EXPIRATION_TOPIC = new QName(SPS_NS_URI, "TaskingRequestExpiration", "sps");
	public static QName REQUEST_REJECTION_TOPIC = new QName(SPS_NS_URI, "TaskingRequestRejection", "sps");
	public static QName REQUEST_ACCEPTANCE_TOPIC = new QName(SPS_NS_URI, "TaskingRequestAcceptance", "sps");
	
	
	public SPSNotificationSystem(String endpointURI, ISubscriptionDB subscriptionDB)
	{
		super(endpointURI, subscriptionDB);
	}

	
	public void notifySubscribers(QName topic, StatusReport report)
	{
		try
		{
			DOMHelper dom = new DOMHelper();
			dom.addUserPrefix("sps", SPS_NS_URI);
			SPSCommonWriterV20 writer = new SPSCommonWriterV20();
			Element reportElt = writer.writeStatusReport(dom, report);
			this.dispatchMessage(topic, reportElt);
		}
		catch (XMLWriterException e)
		{
		}
	}
	
	
	public void notifyTaskSubmitted(StatusReport report)
	{
		notifySubscribers(TASK_SUBMISSION_TOPIC, report);
	}
	
	
	public void notifyTaskCompleted(StatusReport report)
	{
		notifySubscribers(TASK_COMPLETION_TOPIC, report);
	}
	
	
	public void notifyTaskFailed(StatusReport report)
	{
		notifySubscribers(TASK_FAILURE_TOPIC, report);
	}
	
	
	public void notifyTaskCancelled(StatusReport report)
	{
		notifySubscribers(TASK_CANCELLATION_TOPIC, report);
	}
	
	
	public void notifyTaskUpdated(StatusReport report)
	{
		notifySubscribers(TASK_UPDATE_TOPIC, report);
	}
	
	
	public void notifyDataPublished(StatusReport report)
	{
		notifySubscribers(DATA_PUBLICATION_TOPIC, report);
	}
	
	
	public void notifyTaskReservation(StatusReport report)
	{
		notifySubscribers(TASK_RESERVATION_TOPIC, report);
	}
	
	
	public void notifyTaskConfirmation(StatusReport report)
	{
		notifySubscribers(TASK_CONFIRMATION_TOPIC, report);
	}
	
	
	public void notifyReservationExpired(StatusReport report)
	{
		notifySubscribers(RESERVATION_EXPIRATION_TOPIC, report);
	}
	
	
	public void notifyRequestAccepted(StatusReport report)
	{
		notifySubscribers(REQUEST_ACCEPTANCE_TOPIC, report);
	}
	
	
	public void notifyRequestRejected(StatusReport report)
	{
		notifySubscribers(REQUEST_REJECTION_TOPIC, report);
	}
	
	
	public void notifyRequestExpired(StatusReport report)
	{
		notifySubscribers(REQUEST_EXPIRATION_TOPIC, report);
	}
}
