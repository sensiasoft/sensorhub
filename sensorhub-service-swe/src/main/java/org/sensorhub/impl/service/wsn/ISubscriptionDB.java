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

import java.util.Collection;
import java.util.List;


/**
 * <p>
 * Interface for a WSN subscription database
 * </p>
 *
 * <p>Copyright (c) 2014</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Dec 12, 2014
 */
public interface ISubscriptionDB
{
	
    /**
     * Retrieves subscription by ID from database
     * @param id ID of subscription to retrieve
     * @return subscription with specified ID or null if none was found with given ID
     */
    public SubscriptionInfo get(String id);
    
    
    /**
     * Adds the subscription to the database
     * @param sub new subscription
     */
    public void put(SubscriptionInfo sub);
    
    
    /**
     * Removed subscription with given ID from database
     * @param id ID of subscription to remove
     * @return removed subscription or null if none was found with given ID
     */
    public SubscriptionInfo remove(String id);
    
    
    /**
     * Gets all subscriptions in this database
     * @return all subscriptions as a collection
     */
    public Collection<SubscriptionInfo> getAllSubscriptions();
    
    
	/**
	 * Retrieves the list of all subscriptions associated to a given user ID
	 * @param userID
	 * @return subscription list, empty list if the user has no subscriptions attached or null if user ID does not exist 
	 */
	public List<SubscriptionInfo> getUserSubscriptions(String userID);
	
	
	/**
	 * Checks that the suscription is valid for the particular application
	 * For example if a topic is specified it should be one of the topics advertised by the notification service
	 * @param sub
	 * @throws IllegalArgumentException thrown if the subscription is not valid with the appropriate message
	 */
	public void checkSubscription(SubscriptionInfo sub) throws IllegalArgumentException;
	
	
	/**
	 * Generates a new unique ID for a new subscription
	 * @return generated ID
	 */
	public String generateNewSubscriptionID();
}
