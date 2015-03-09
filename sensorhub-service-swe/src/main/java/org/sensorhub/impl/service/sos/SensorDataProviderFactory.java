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

import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.utils.MsgUtils;
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataProvider;


/**
 * <p>
 * Factory for sensor data providers.<br/>
 * Most of the logic is inherited from {@link StreamDataProviderFactory}.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 15, 2013
 */
public class SensorDataProviderFactory extends StreamDataProviderFactory<ISensorModule<?>> implements IDataProviderFactory, IEventListener
{
    
    
    protected SensorDataProviderFactory(SensorDataProviderConfig config) throws SensorHubException
    {
        super(config,
              SensorHub.getInstance().getSensorManager().getModuleById(config.sensorID),
              "Sensor");
    }
    
    
    @Override
    public AbstractProcess generateSensorMLDescription(double time) throws ServiceException
    {
        checkEnabled();
        
        try
        {
            if (Double.isNaN(time))
                return producer.getCurrentDescription();
            else
                return producer.getSensorDescription(time);
        }
        catch (SensorException e)
        {
            throw new ServiceException("Cannot retrieve SensorML description of sensor " + MsgUtils.moduleString(producer), e);
        }
    }

    
    @Override
    public ISOSDataProvider getNewProvider(SOSDataFilter filter) throws ServiceException
    {
        checkEnabled();
        return new SensorDataProvider(producer, filter);
    }
}
