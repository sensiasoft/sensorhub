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

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
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
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 15, 2014
 */
public class SensorWithStorageProviderFactory extends StorageDataProviderFactory
{
    final ISensorModule<?> sensor;
    double liveDataTimeOut;
    
    
    public SensorWithStorageProviderFactory(SensorDataProviderConfig config) throws SensorHubException
    {
        super(new StorageDataProviderConfig(config));
        
        // get handle to sensor instance using sensor manager
        this.sensor = SensorHub.getInstance().getSensorManager().getModuleById(config.sensorID);
        this.liveDataTimeOut = config.liveDataTimeout;
    }


    @Override
    public ISOSDataProvider getNewProvider(SOSDataFilter filter) throws ServiceException
    {
        TimeExtent timeRange = filter.getTimeRange();
        
        if (timeRange.isBaseAtNow() || timeRange.isBeginNow())
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
        
        // enable real-time requests if sensor is enabled
        if (sensor.isEnabled())
        {
            TimeExtent storageTimeExtent = caps.getPhenomenonTime();
            if (storageTimeExtent.isNull())
                caps.getPhenomenonTime().setBaseAtNow(true);
            else            
                caps.getPhenomenonTime().setEndNow(true);        
        }
        
        return capabilities;
    }
    
    
    @Override
    public void updateCapabilities() throws ServiceException
    {
        super.updateCapabilities();
        
        // enable real-time requests if sensor is enabled
        if (sensor.isEnabled())
        {
            try
            {
                // check latest record time
                double lastRecordTime = Double.NEGATIVE_INFINITY;
                for (ISensorDataInterface output: sensor.getAllOutputs().values())
                {
                    // skip hidden outputs
                    if (config.hiddenOutputs != null && config.hiddenOutputs.contains(output.getName()))
                        continue;
                    
                    double recTime = output.getLatestRecordTime();
                    if (!Double.isNaN(recTime) && recTime > lastRecordTime)
                        lastRecordTime = recTime;
                }
                
                // if latest record is not too old, enable real-time
                double now =  System.currentTimeMillis() / 1000.; 
                if (now - lastRecordTime < liveDataTimeOut)
                    caps.getPhenomenonTime().setEndNow(true);
            }
            catch (SensorException e)
            {
                throw new ServiceException("Error while updating capabilities", e);
            }        
        }
    }
}
