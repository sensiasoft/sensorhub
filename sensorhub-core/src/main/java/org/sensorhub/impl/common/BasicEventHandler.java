/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.common;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;


/**
 * <p>
 * Basic implementation of a synchronous event manager.
 * </p><p>
 * We have to take care of cases of calls to register/unregister initiated
 * synchronously (in same thread) by listeners while an event is being dispatched.
 * This is done by using temporary lists and committing changes that occured during
 * the iteration only at the end of the call to publish().
 * </p><p>
 * Likewise recursive calls to publish have to be handled as well. This is done 
 * by accumulating events in a queue and dispatching each one in their own loop,
 * thus avoiding recursive calls to publish while iterating.
 * </p><p>
 * We use weak references in the main list of listeners to prevent memory leaks in
 * cases where listeners forget to unregister themselves.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 16, 2010
 */
public class BasicEventHandler implements IEventHandler
{
    List<WeakReference<IEventListener>> listeners = new ArrayList<WeakReference<IEventListener>>();
    List<IEventListener> toDelete = new ArrayList<IEventListener>();
    List<IEventListener> toAdd = new ArrayList<IEventListener>();
    Deque<Event<?>> eventQueue = new LinkedBlockingDeque<Event<?>>();
    boolean inPublish = false;
    
    
    @Override
    public synchronized void registerListener(IEventListener listener)
    {
        if (!listeners.contains(listener))
        {
            // add directly or through temporary list if publishing
            if (!inPublish)
                addWeakRef(listener);
            else
                toAdd.add(listener);
        }
    }


    @Override
    public synchronized void unregisterListener(IEventListener listener)
    {
        // remove directly or through temporary list if publishing
        if (!inPublish)
            removeWeakRef(listener);
        else
            toDelete.add(listener);
    }
    
    
    @Override
    public synchronized void publishEvent(Event<?> e)
    {
        // case of recursive call
        if (inPublish)
        {
            eventQueue.addLast(e);
        }
        else
        {        
            try
            {
                inPublish = true;
                for (Iterator<WeakReference<IEventListener>> it = listeners.iterator(); it.hasNext(); )
                {
                    IEventListener listener = it.next().get();
                    if (listener != null)
                        listener.handleEvent(e);
                    else
                        it.remove(); // purge cleared references
                }
            }
            finally
            {
                // make sure we end our publish session even in case of uncaught error
                inPublish = false;
                commitChanges();
            }
        }
    }
    
    
    private final void commitChanges()
    {
        commitRemoves();
        commitAdds();
        
        while (!eventQueue.isEmpty())
            publishEvent(eventQueue.pollFirst());
    }
    
    
    private final void commitAdds()
    {
        for (IEventListener listener: toAdd)
            addWeakRef(listener);
        toAdd.clear();
    }
    
    
    private final void commitRemoves()
    {
        for (IEventListener listener: toDelete)
            removeWeakRef(listener);
        toDelete.clear();
    }
    
    
    private final void addWeakRef(IEventListener listener)
    {
        listeners.add(new WeakReference<IEventListener>(listener));
    }
    
    
    private final void removeWeakRef(IEventListener listenerToRemove)
    {
        for (Iterator<WeakReference<IEventListener>> it = listeners.iterator(); it.hasNext(); )
        {
            IEventListener listener = it.next().get();
            if (listener == null || listener == listenerToRemove)  // also purge cleared references
                it.remove();
        }
    }


    @Override
    public void clearAllListeners()
    {
        listeners.clear();        
    }
}
