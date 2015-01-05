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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Iterator;
import javax.xml.namespace.QName;
import org.apache.muse.ws.addressing.EndpointReference;
import org.apache.muse.ws.addressing.soap.SoapFault;
import org.apache.muse.ws.notification.Filter;
import org.apache.muse.ws.notification.impl.FilterFactory;
import org.apache.muse.ws.notification.impl.PublishAllMessagesFilter;
import org.apache.muse.ws.notification.impl.SimpleNotificationMessage;
import org.apache.muse.ws.notification.impl.Subscribe;
import org.apache.muse.ws.notification.impl.SubscribeResponse;
import org.apache.muse.ws.notification.remote.NotificationConsumerClient;
import org.apache.muse.ws.resource.WsResource;
import org.apache.muse.ws.resource.faults.ResourceUnknownFault;
import org.apache.muse.ws.resource.impl.SimpleWsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


/**
 * Helper class to send notification messages in the
 * WS-Notification format. It relies on the Apache
 * MUSE library version 2.2.0.
 * 
 * @author Alexandre Robin <alexandre.robin@spotimage.fr>
 */
public class NotificationSystem
{
	public static int UNLIMITED_DURATION = -1;
	
	protected static String wsnUri = "http://docs.oasis-open.org/wsn/b-2";
	protected static Logger log = LoggerFactory.getLogger(NotificationSystem.class);
	
	protected ISubscriptionDB subscriptionDB;
	protected EndpointReference producerEPR;
	protected long maxSubscriptionLength = UNLIMITED_DURATION; // in seconds
	protected int timeBetweenRetries = 2;
	
	
	public NotificationSystem(String endpointURI, ISubscriptionDB subscriptionDB)
	{
		this.subscriptionDB = subscriptionDB;
		
		try
		{
			if (!endpointURI.endsWith("/"))
				endpointURI += "/";
			producerEPR = new EndpointReference(new URI(endpointURI));
		}
		catch (URISyntaxException e)
		{
			log.error("Invalid notification service endpoint", e);
			throw new IllegalStateException();
		}
		
		FilterFactory.getInstance().addHandler(new TopicListFilterHandler());
	}
	
	
	public long getMaxSubscriptionLength()
	{
		return maxSubscriptionLength;
	}
	
	
	/**
	 * Sets the initial duration of all subscriptions after which they are removed from the DB.
	 * This is a service wide value but users can increase the duration of their subscriptions by renewing their subscription.
	 * @param subscriptionLength  max subscription duration in seconds. Use the UNLIMITED_DURATION constant for unlimited duration.
	 */
	public void setMaxSubscriptionLength(long subscriptionLength)
	{
		this.maxSubscriptionLength = subscriptionLength;
	}
	
	
	public int getTimeBetweenRetries()
	{
		return timeBetweenRetries;
	}


	/**
	 * Sets the time to wait between two successive retries
	 * @param timeBetweenRetries
	 */
	public void setTimeBetweenRetries(int timeBetweenRetries)
	{
		this.timeBetweenRetries = timeBetweenRetries;
	}


	/**
	 * Call this method with the content of the Subscribe message received by the service.
	 * If the service is using a SOAP binding, this method should be called with the content of the body element
	 * @param requestElt
	 * @return DOM element containing SubsribeResponse
	 * @throws Exception
	 */
	public synchronized Element subscribe(Element requestElt) throws Exception
	{
		Subscribe request = new Subscribe(requestElt);				
		Filter filter = request.getFilter();
		EndpointReference consumer = request.getConsumerReference();
		
		if (consumer == null)
            throw new NullPointerException("NullConsumerEPR");
        
        if (filter == null)
            filter = PublishAllMessagesFilter.getInstance();
        
        // generate random subscription UUID
        String subUUID = subscriptionDB.generateNewSubscriptionID();
        
        // create subscription and add to DB
        SubscriptionInfo newSub = new SubscriptionInfo(subUUID);
        newSub.setEndpoint(createSubscriptionReference(subUUID));
        newSub.setProducer(producerEPR);
        newSub.setConsumer(consumer);
        newSub.setFilter(filter);
        
        long maxDate = System.currentTimeMillis() + maxSubscriptionLength*1000;
        if (maxSubscriptionLength == UNLIMITED_DURATION)
        	newSub.setTerminationTime(request.getTerminationTime());
        else if (request.getTerminationTime().getTime() < maxDate)
        	newSub.setTerminationTime(request.getTerminationTime());
        else
        	newSub.setTerminationTime(new Date(maxDate));
        
        subscriptionDB.checkSubscription(newSub);
        subscriptionDB.put(newSub);
        	
		// create response
		WsResource subRes = new SimpleWsResource();
		subRes.setEndpointReference(newSub.getEndpoint());
		SubscribeResponse response = new SubscribeResponse(subRes, newSub.getTerminationTime());

		return response.toXML();
	}
	
	
	public synchronized Element renew(String subUUID, Element requestElt) throws Exception
	{
		// reset manager date
		SubscriptionInfo sub = subscriptionDB.get(subUUID);
		sub.terminationTime = new Date(System.currentTimeMillis() + maxSubscriptionLength*1000);
		
		// create response message
		Element respElt = requestElt.getOwnerDocument().createElementNS(wsnUri, "wsnt:RenewResponse");		
		return respElt;
	}
	
	
	public synchronized Element unsubscribe(String subUUID, Element requestElt) throws Exception
	{
		// find subscription in table
		if (subscriptionDB.remove(subUUID) == null)
			throw new ResourceUnknownFault("Unknown Resource UUID: " + subUUID);
		
		// create response message
		Element respElt = requestElt.getOwnerDocument().createElementNS(wsnUri, "wsnt:UnsubscribeResponse");	
		return respElt;
	}
	
	
	public synchronized void dispatchMessage(QName topic, Element messageElt)
	{
		// construct message
		SimpleNotificationMessage msg = new SimpleNotificationMessage();
		msg.setTopic(topic);
		msg.addMessageContent(messageElt);
		log.debug("Dispatching Message\n" + msg);
		
		// send WSN notification message to all subscribers
        // remove subscriptions if they are too old
        long now = new Date().getTime();
        Iterator<SubscriptionInfo> it = subscriptionDB.getAllSubscriptions().iterator();
		while (it.hasNext())
		{
			SubscriptionInfo nextSub = it.next();
			
			try
			{
				// message filtering
				if (!nextSub.getFilter().accepts(msg))
				{
					log.debug("Not matching filter of " + nextSub.getConsumer().getAddress());
					continue;
				}
				
				long endTime = nextSub.getTerminationTime().getTime();
				if (endTime > 0 && now >= endTime)
					it.remove();
				else
				{
					int tries = 0;
					while (tries < nextSub.getNumberOfTries())
					{
						try
						{
							msg.setSubscriptionReference(nextSub.getEndpoint());
							NotificationConsumerClient client = new NotificationConsumerClient(nextSub.getConsumer(), nextSub.getProducer());
							client.notify(msg);
							break;
						}
						catch (SoapFault fault)
						{
							// HACK to work around bug in MUSE library
							if (fault.getReason().contains("Premature end of file"))
								break;
							
							if (tries == nextSub.getNumberOfTries())
								log.error("Could not reach notification consumer endpoint at " + nextSub.getConsumer().getAddress());
						}
						
						log.warn("Retrying to send notification to " + nextSub.getConsumer().getAddress() + " in " + timeBetweenRetries + "s");
						Thread.sleep(timeBetweenRetries*1000);
						tries++;
					}
					
					log.debug("Dispatched to " + nextSub.getConsumer().getAddress());
					log.debug(msg.toString());
				}
			}
			catch (Exception e)
			{
				log.error("Error while sending notification message to subscriber " + nextSub.getConsumer(), e);
			}
		}
	}
	
	
	protected EndpointReference createSubscriptionReference(String subUUID)
	{
		try
		{
			URI idPath = new URI(subUUID);
			URI subURI = producerEPR.getAddress().resolve(idPath);
			EndpointReference subEPR = new EndpointReference(subURI);
			return subEPR;
		}
		catch (URISyntaxException e)
		{
			log.error("Cannot build EPR from subscription ID", e);
			throw new IllegalStateException();
		}		
	}
}
