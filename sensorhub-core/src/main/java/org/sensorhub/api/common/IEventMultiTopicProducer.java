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
 * Common interface for all event producers that can produce events on multiple
 * topic channels.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 19, 2015
 */
public interface IEventMultiTopicProducer
{
    
    /**
     * Registers a listener with the given topic.
     * The listener is reponsible for filtering received events.
     * @param topic topic identifier
     * @param listener
     * @return true if topic is supported by dispatcher
     */
    public boolean registerListener(String topic, IEventListener listener);


    /**
     * Unregisters a listener from this event producer.
     * No more event will be sent to the listener from this producer.
     * @param topic topic identifier
     * @param listener
     */
    public void unregisterListener(String topic, IEventListener listener);
    
}
