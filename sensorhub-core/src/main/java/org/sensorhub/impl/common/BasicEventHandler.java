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

package org.sensorhub.impl.common;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;


/**
 * <p>
 * Basic synchronous event manager.
 * We still have to take care of cases of calls to register/unregister
 * initiated synchronously (in same thread) by listeners while an event is being 
 * dispatched. This is done by using temporary lists and committing changes that
 * occured during the iteration only at the end of the call to publish().
 * Likewise recursive calls to publish have to be handled as well. This is done 
 * by accumulating events in a queue and dispatching them one by one.
 * </p>
 *
 * <p>Copyright (c) 2014</p>
 * @author Alexandre Robin
 * @since Nov 16, 2010
 */
public class BasicEventHandler implements IEventHandler
{
    List<IEventListener> listeners = new ArrayList<IEventListener>();
    List<IEventListener> toDelete = new ArrayList<IEventListener>();
    List<IEventListener> toAdd = new ArrayList<IEventListener>();
    Deque<Event> eventQueue = new LinkedBlockingDeque<Event>();
    boolean inPublish = false;
    
    
    @Override
    public synchronized void registerListener(IEventListener listener)
    {
        if (!listeners.contains(listener))
        {
            // add directly or through temporary list if publishing
            if (!inPublish)
                listeners.add(listener);
            else
                toAdd.add(listener);
        }
    }


    @Override
    public synchronized void unregisterListener(IEventListener listener)
    {
        // remove directly or through temporary list if publishing
        if (!inPublish)
            listeners.remove(listener);
        else
            toDelete.add(listener);
    }
    
    
    @Override
    public synchronized void publishEvent(Event e)
    {
        // case of recursive call
        if (inPublish)
        {
            eventQueue.addLast(e);
        }
        else
        {        
            inPublish = true;
            for (IEventListener listener: listeners)
                listener.handleEvent(e);
            inPublish = false;
            commitChanges();
        }
    }
    
    
    private void commitChanges()
    {
        listeners.removeAll(toDelete);
        listeners.addAll(toAdd);
        toDelete.clear();
        toAdd.clear();
        
        while (!eventQueue.isEmpty())
            publishEvent(eventQueue.pollFirst());
    }


    @Override
    public void clearAllListeners()
    {
        listeners.clear();        
    }
}
