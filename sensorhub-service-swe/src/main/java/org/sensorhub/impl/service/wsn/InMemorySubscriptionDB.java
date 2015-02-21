/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.wsn;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


/**
 * <p>
 * Default implementation of subscription DB backed up by a hash map that is
 * fully stored in memory.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Dec 12, 2014
 */
public class InMemorySubscriptionDB extends HashMap<String, SubscriptionInfo> implements ISubscriptionDB
{
	private static final long serialVersionUID = 1L;


	public void checkSubscription(SubscriptionInfo sub) throws IllegalArgumentException
	{		
	}


	@Override
    public SubscriptionInfo get(String id)
    {
        return super.get(id);
    }
	
	
	@Override
    public void put(SubscriptionInfo sub)
    {
        super.put(sub.getUid(), sub);
    }


    @Override
    public SubscriptionInfo remove(String id)
    {
        return super.remove(id);
    }
    
    
    @Override
    public String generateNewSubscriptionID()
	{
		return UUID.randomUUID().toString();
	}


	@Override
    public Collection<SubscriptionInfo> getAllSubscriptions()
    {
        return this.values();
    }
	
	
	@Override
    public List<SubscriptionInfo> getUserSubscriptions(String userID)
	{
		return null;
	}
}
