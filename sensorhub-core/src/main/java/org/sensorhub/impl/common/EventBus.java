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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;


/**
 * <p>
 * Event Bus: Main event management class in SensorHub.<br/>
 * All event producers and listeners registrations must be done through
 * this class (instead of directly with the source module) in order to
 * benefit from more advanced event dispatching implementations such as
 * distributed event messaging.<br/>
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 19, 2015
 */
public class EventBus
{
    public static final String MAIN_TOPIC = "_MAIN"; 
        
    private Map<String, IEventHandler> eventHandlers;
    private ExecutorService threadPool;
    
    
    public EventBus()
    {
        eventHandlers = new HashMap<String, IEventHandler>();
        
        // create thread pool that will be used by all asynchronous event handlers
        threadPool = new ThreadPoolExecutor(0, 100,
                                            10L, TimeUnit.SECONDS,
                                            new SynchronousQueue<Runnable>(),
                                            new DefaultThreadFactory("EventBus"));
    }
    
    
    public synchronized IEventHandler registerProducer(String moduleID)
    {
        return registerProducer(moduleID, MAIN_TOPIC);
    }
    
    
    public synchronized IEventHandler registerProducer(String moduleID, String topic)
    {
        String key = getKey(moduleID, topic);
        return ensureHandler(key);
    }
    
    
    public synchronized IEventHandler registerProducer(String moduleID, String topic, IEventHandler handlerImpl)
    {
        String key = getKey(moduleID, topic);
        
        // return already registered handler if any
        IEventHandler handler = eventHandlers.get(key);
        if (handler != null)
            return handler;
        
        // otherwise register the provided handler
        if (handlerImpl != null)
            eventHandlers.put(key, handlerImpl);
        
        return handlerImpl;
    }
    
    
    public synchronized void unregisterProducer(String moduleID, String topic)
    {
        String key = getKey(moduleID, topic);
        eventHandlers.remove(key);
    }
    
    
    public synchronized void registerListener(String moduleID, String topic, IEventListener listener)
    {
        String key = getKey(moduleID, topic);
        // ensure the handler is created so we can register a listener before a producer!
        IEventHandler handler = ensureHandler(key);
        handler.registerListener(listener);
    }


    public synchronized void unregisterListener(String moduleID, String topic, IEventListener listener)
    {
        String key = getKey(moduleID, topic);
        IEventHandler handler = eventHandlers.get(key);
        if (handler != null)
            handler.unregisterListener(listener);
    }
    
    
    private final IEventHandler ensureHandler(String key)
    {
        IEventHandler handler = eventHandlers.get(key);
        
        // register new handleronly if non already exist for this key
        if (handler == null)
        {
            //handler = new BasicEventHandler();
            handler = new AsyncEventHandler(threadPool);
            eventHandlers.put(key, handler);
        }
        
        return handler;
    }
    
    
    private final String getKey(String moduleID, String topic)
    {
        return moduleID + topic;
    }

    
    public void shutdown()
    {
        if (threadPool != null)
            threadPool.shutdownNow();
    }
}
