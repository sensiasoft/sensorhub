/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.api.service.ServiceException;
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataProvider;


/**
 * <p>
 * Implementation of SOS data provider connecting to a sensor via 
 * SensorHub's sensor API (ISensorDataInterface).<br/>
 * Most of the logic is inherited from {@link StreamDataProvider}.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class SensorDataProvider extends StreamDataProvider implements ISOSDataProvider, IEventListener
{

    
    public SensorDataProvider(ISensorModule<?> srcSensor, SensorDataProviderConfig config, SOSDataFilter filter) throws ServiceException
    {
        super(srcSensor, config, filter);
    }
    
    
    @Override
    public void handleEvent(Event<?> e)
    {
        if (e instanceof SensorEvent)
        {
            if (((SensorEvent) e).getType() == SensorEvent.Type.DISCONNECTED)
            {
                
            }
        }
        else
            super.handleEvent(e);
    }
}
