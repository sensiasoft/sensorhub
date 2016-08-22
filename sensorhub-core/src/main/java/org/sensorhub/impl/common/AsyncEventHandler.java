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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Asynchronous event handler implementation.<br/>
 * This implementation keeps one queue per listener to avoid slowing down dispatching
 * events to other listeners in case one listener has a higher processing time.
 * It also ensures that events are delivered to each listener in the order they
 * were received.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 30, 2013
 */
public class AsyncEventHandler implements IEventHandler
{
    private static final Logger log = LoggerFactory.getLogger(AsyncEventHandler.class);
    private Map<IEventListener, ListenerQueue> listeners;
    private ExecutorService threadPool;
    
    
    // helper class to use one queue per listener so they don't slow down each other
    class ListenerQueue
    {
        Queue<Event<?>> eventQueue = new ConcurrentLinkedQueue<Event<?>>();
        volatile boolean dispatching = false;
        
        void dispatchNextEvent(final IEventListener listener)
        {
            // dispatch next event from queue
            final Event<?> e = eventQueue.poll();
            if (e != null)
            {
                dispatching = true;
                
                threadPool.execute(new Runnable() {
                    public void run()
                    {
                        try
                        {
                            long dispatchDelay = System.currentTimeMillis() - e.getTimeStamp();
                            if (dispatchDelay > 100)
                            {
                                String srcName = e.getSource().getClass().getSimpleName();
                                String destName = listener.getClass().getSimpleName();
                                log.warn("{} Event from {} to {} @ {}, dispatch delay={}, queue size={}", e.getType(), srcName, destName, e.getTimeStamp(), dispatchDelay, eventQueue.size());
                            }                            
                            
                            //String srcName = e.getSource().getClass().getSimpleName();
                            //String destName = listener.getClass().getSimpleName();
                            //log.debug("Thread {}: Dispatching {} event from {} to {} @ {}, dispatch delay={}, queue size={}", Thread.currentThread().getId(), e.getType(), srcName, destName, e.getTimeStamp(), dispatchDelay, eventQueue.size());
                            
                            listener.handleEvent(e);
                        }
                        catch (Exception ex)
                        {
                            String srcName = e.getSource().getClass().getSimpleName();
                            String destName = listener.getClass().getSimpleName();
                            log.error("Uncaught exception while dispatching event from {} to {}", srcName, destName, ex);
                        }   
                        finally
                        {
                            dispatchNextEvent(listener);                                
                        }
                    }                        
                });
            }
            else
                dispatching = false;
        }
        
        boolean pushEvent(final Event<?> e)
        {
            return eventQueue.offer(e);                
        }
    }
    
    
    public AsyncEventHandler(ExecutorService threadPool)
    {
        this.threadPool = threadPool;
        this.listeners = new WeakHashMap<IEventListener, ListenerQueue>();
    }
    
    
    @Override
    public void publishEvent(final Event<?> e)
    {
        synchronized (listeners)
        {
            // add event to queue of each listener
            for (Entry<IEventListener, ListenerQueue> entry: listeners.entrySet())
            {
                IEventListener listener = entry.getKey();
                ListenerQueue queue = entry.getValue();
                
                // add event to each listener queue
                if (!queue.pushEvent(e))
                {
                    String srcName = e.getSource().getClass().getSimpleName();
                    String destName = listener.getClass().getSimpleName();
                    log.error("Max queue size reached when dispatching event from {} to {}. Clearing queue", srcName, destName);
                    queue.eventQueue.clear();
                    return;
                }
                
                // dispatch next event
                if (!queue.dispatching)
                    queue.dispatchNextEvent(listener);
            }
        }
    }
   

    @Override
    public void registerListener(IEventListener listener)
    {
        synchronized (listeners)
        {
            listeners.put(listener, new ListenerQueue());
        }
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        synchronized (listeners)
        {
            listeners.remove(listener);
        }
    }
    
    
    @Override
    public int getNumListeners()
    {
        synchronized (listeners)
        {
            return listeners.size();
        }
    }
    
    
    @Override
    public void clearAllListeners()
    {
        synchronized (listeners)
        {
            listeners.clear();
        }
    }
}
