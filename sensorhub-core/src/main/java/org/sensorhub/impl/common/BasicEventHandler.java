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
import java.util.List;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;


/**
 * <p>
 * Basic synchronous event manager
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 16, 2010
 */
public class BasicEventHandler implements IEventHandler
{
    protected List<IEventListener> listeners = new ArrayList<IEventListener>();
    
    
    /* (non-Javadoc)
     * @see org.sensorhub.impl.common.IEventHandler#registerListener(org.sensorhub.api.common.IEventListener)
     */
    @Override
    public void registerListener(IEventListener listener)
    {
        listeners.add(listener);
    }


    /* (non-Javadoc)
     * @see org.sensorhub.impl.common.IEventHandler#removeListener(org.sensorhub.api.common.IEventListener)
     */
    @Override
    public void unregisterListener(IEventListener listener)
    {
        listeners.remove(listener);
    }
    
    
    /* (non-Javadoc)
     * @see org.sensorhub.impl.common.IEventHandler#publishEvent(org.sensorhub.api.common.Event)
     */
    @Override
    public void publishEvent(Event e)
    {
        for (IEventListener listener: listeners)
            listener.handleEvent(e);
    }
}
