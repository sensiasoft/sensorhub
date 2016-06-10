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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;


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
    private Thread dispatchThread;
    private BlockingQueue<Event<?>> eventQueue;
    private List<WeakReference<IEventListener>> listeners;
    private boolean started;
    private boolean paused;
    
    
    public AsyncEventHandler()
    {
        this.eventQueue = new LinkedBlockingQueue<Event<?>>();
        this.listeners = new ArrayList<WeakReference<IEventListener>>();
        
        dispatchThread = new Thread() {            
            public void run()
            {                
                while (started)
                {
                    synchronized (this)
                    {
                        Event<?> e = eventQueue.poll();
                        
                        // call all listeners
                        for (Iterator<WeakReference<IEventListener>> it = listeners.iterator(); it.hasNext(); )
                        {
                            IEventListener listener = it.next().get();
                            if (listener != null)
                                listener.handleEvent(e);
                            else
                                it.remove(); // purge cleared references
                        }
                        
                        try
                        {
                            while (paused)
                                this.wait();
                        }
                        catch (InterruptedException e1)
                        {
                        }
                    }
                }
            }
        };
    }
    
    
    @Override
    public void publishEvent(Event<?> e)
    {
        try
        {
            eventQueue.put(e);
        }
        catch (InterruptedException e1)
        {
        }
    }
    
    
    public synchronized void start()
    {
        this.started = true;
        this.paused = false;
        dispatchThread.start();
    }
    
    
    public synchronized void stop()
    {
        this.started = false;
        this.paused = false;
        this.notify();
    }
    
    
    public synchronized void pause()
    {
        this.paused = true;
    }
   

    @Override
    public synchronized void registerListener(IEventListener listener)
    {
        listeners.add(new WeakReference<IEventListener>(listener));
    }


    @Override
    public synchronized void unregisterListener(IEventListener listener)
    {
        for (Iterator<WeakReference<IEventListener>> it = listeners.iterator(); it.hasNext(); )
        {
            IEventListener l = it.next().get();
            if (l == null || l == listener)  // also purge cleared references
                it.remove();
        }
    }
    
    
    @Override
    public synchronized int getNumListeners()
    {
        int count = 0;
        for (WeakReference<IEventListener> listener: listeners)
        {
            if (listener.get() != null)
                count++;
        }
        
        return count;
    }
    
    
    @Override
    public synchronized void clearAllListeners()
    {
        listeners.clear();
    }
}
