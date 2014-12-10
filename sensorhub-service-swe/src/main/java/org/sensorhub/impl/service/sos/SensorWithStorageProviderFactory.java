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

package org.sensorhub.impl.service.sos;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.utils.MsgUtils;
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Factory for sensor data providers with storage.
 * </p>
 * <p>
 * This factory is associated to an SOS offering and is persistent
 * throughout the lifetime of the service, so it must be threadsafe.
 * </p>
 * <p>
 * However, the server will obtain a new data provider instance from this
 * factory for each incoming request so the providers themselves don't need
 * to be threadsafe. 
 * </p>
 *
 * <p>Copyright (c) 2014</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Nov 15, 2014
 */
public class SensorWithStorageProviderFactory extends StorageDataProviderFactory
{
    final ISensorModule<?> sensor;
    
    
    public SensorWithStorageProviderFactory(SensorDataProviderConfig config) throws SensorHubException
    {
        super(new StorageDataProviderConfig(config));
        
        // get handle to sensor instance using sensor manager
        this.sensor = SensorHub.getInstance().getSensorManager().getModuleById(config.sensorID);
    }


    @Override
    public ISOSDataProvider getNewProvider(SOSDataFilter filter) throws ServiceException
    {
        if (filter.getTimeRange().isBaseAtNow())
        {
            if (!sensor.isEnabled())
                throw new ServiceException("Sensor " + MsgUtils.moduleString(sensor) + " is disabled");
            
            return new SensorDataProvider(sensor, filter);
        }
        else
        {            
            return super.getNewProvider(filter);
        }
    }


    @Override
    public boolean isEnabled()
    {
        return config.enabled && (sensor.isEnabled() || storage.isEnabled());
    }


    @Override
    public SOSOfferingCapabilities generateCapabilities() throws ServiceException
    {
        SOSOfferingCapabilities capabilities = super.generateCapabilities();
        
        if (sensor.isEnabled())
        {
            // enable real-time time requests
            TimeExtent timeExtent = new TimeExtent();
            timeExtent.setBaseAtNow(true);
            capabilities.getPhenomenonTimes().add(timeExtent);
        }
        
        return capabilities;
    }
}
