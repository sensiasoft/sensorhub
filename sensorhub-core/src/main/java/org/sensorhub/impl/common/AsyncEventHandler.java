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

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
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
 * This basic implementation just collects events into a queue and dispatch 
 * them to all listeners in a separate thread.<br/>
 * The queue size is set to {@link Integer#MAX_VALUE} so memory consumption can be
 * very high if events are not processed fast enough.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 30, 2013
 */
public class AsyncEventHandler implements IEventHandler
{
    private static final Logger log = LoggerFactory.getLogger(AsyncEventHandler.class);
    private ExecutorService threadPool;
    private Queue<Event<?>> eventQueue;
    private Set<IEventListener> listeners;
    private int dispatchLeft = 0;
    
    
    public AsyncEventHandler(ExecutorService threadPool)
    {
        this.threadPool = threadPool;
        this.eventQueue = new ConcurrentLinkedQueue<Event<?>>();
        this.listeners = Collections.newSetFromMap(new WeakHashMap<IEventListener, Boolean>());
    }
    
    
    @Override
    public void publishEvent(final Event<?> e)
    {
        // do nothing if we have no listeners
        synchronized (listeners)
        {
            if (listeners.isEmpty())
                return;
        }
            
        // add event to queue
        if (!eventQueue.offer(e))
            throw new RuntimeException("Max event queue size reached");
        
        dispatchNextEvent();
    }
    
    
    protected synchronized void dispatchNextEvent()
    {
        // do nothing if we're still dispatching previous event
        if (dispatchLeft > 0)
            return;
        
        // dispatch next event from queue
        final Event<?> e = eventQueue.poll();        
        if (e != null)
        {
            synchronized (listeners)
            {
                dispatchLeft = listeners.size();
                
                // call all listeners
                for (final IEventListener listener: listeners)
                {
                    threadPool.execute(new Runnable() {
                        public void run()
                        {
                            try
                            {
                                long dispatchDelay = System.currentTimeMillis()-e.getTimeStamp();
                                if (dispatchDelay > 100)
                                    log.warn("{} Event from {} @ {}, dispatch delay={}, queue size={}", e.getType(), e.getSource().getClass().getSimpleName(), e.getTimeStamp(), dispatchDelay, eventQueue.size());
                                
                                listener.handleEvent(e);
                            }
                            catch (Exception ex)
                            {
                                log.error("Uncaught exception while dispatching event", ex);
                            }   
                            finally
                            {
                                dispatchDone();                                
                            }
                        }                        
                    });
                }
            }
        }
    }
    
    
    protected synchronized void dispatchDone()
    {
        dispatchLeft--;
        if (dispatchLeft == 0)
            dispatchNextEvent();
    }
   

    @Override
    public void registerListener(IEventListener listener)
    {
        synchronized (listeners)
        {
            listeners.add(listener);
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
