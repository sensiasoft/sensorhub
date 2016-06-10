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
 * Interface for all event handling classes.
 * Typically, event dispatching is delegated by event producers to 
 * implementations of this interface. It accepts registering/unregistering of
 * listeners and also provide a publish method to be called by the producer
 * for dispatching the events.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2013
 */
public interface IEventHandler extends IEventProducer
{

    /**
     * Dispatch event to all registered listeners
     * @param e event to dispatch
     */
    public void publishEvent(Event<?> e);
    
    
    /**
     * @return the number of listeners currently registered
     */
    public int getNumListeners();
    
    
    /**
     * Clear all listeners.
     * Usually called on producer side during cleanup phase
     */
    public void clearAllListeners();

}