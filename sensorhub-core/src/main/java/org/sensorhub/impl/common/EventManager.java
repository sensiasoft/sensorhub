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

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.common.IEventMultiTopicProducer;
import org.sensorhub.api.common.IEventListener;


/**
 * <p>
 * Main event management class in SensorHub.<br/>
 * All listener registrations must be done through this class (instead of
 * directly with the source module) in order to benefit from more advanced
 * event dispatching implementations such as distributed event messaging.<br/>
 * This class keeps a list of delegate dispatchers which can broadcast events
 * to local and remote listeners.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 19, 2015
 */
public class EventManager implements IEventMultiTopicProducer
{
    private static EventManager singletonInstance;
    private List<IEventMultiTopicProducer> topicDelegates;
    
    
    public static EventManager getInstance()
    {
        if (singletonInstance == null)
            singletonInstance = new EventManager();
        
        return singletonInstance;
    }
    
    
    private EventManager()
    {
        topicDelegates = new ArrayList<IEventMultiTopicProducer>();
    }
    
    
    @Override
    public boolean registerListener(String topic, IEventListener listener)
    {
        // register with first dispatcher accepting the topic
        for (IEventMultiTopicProducer dispatcher: topicDelegates) {
            if (dispatcher.registerListener(topic, listener))
                return true;
        }
        
        return false;
    }


    @Override
    public void unregisterListener(String topic, IEventListener listener)
    {
        for (IEventMultiTopicProducer dispatcher: topicDelegates)
            dispatcher.registerListener(topic, listener);
    }
    
    
    public void addDelegateDispatcher(IEventMultiTopicProducer dispatcher)
    {
        topicDelegates.add(dispatcher);
    }

}
