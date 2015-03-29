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
 * Base interface for all objects accepting producing events and accepting
 * registration of event listeners 
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2013
 */
public interface IEventProducer
{
    
    /**
     * Registers a listener with this event producer.
     * The listener is reponsible for filtering received events.
     * @param listener
     */
    public void registerListener(IEventListener listener);


    /**
     * Unregisters a listener from this event producer.
     * No more event will be sent to the listener from this producer.
     * @param listener
     */
    public void unregisterListener(IEventListener listener);
}
