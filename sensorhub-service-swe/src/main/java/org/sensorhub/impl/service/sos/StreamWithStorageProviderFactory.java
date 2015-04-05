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
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.service.ServiceException;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Factory for streaming data providers with storage.<br/>
 * Most of the logic is inherited from {@link StorageDataProviderFactory}.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ProducerType> Type of producer handled by this provider
 * @since Feb 28, 2015
 */
public class StreamWithStorageProviderFactory<ProducerType extends IDataProducerModule<?>> extends StorageDataProviderFactory
{
    final ProducerType producer;
    long liveDataTimeOut;
    
    
    public StreamWithStorageProviderFactory(StreamDataProviderConfig config, ProducerType producer) throws SensorHubException
    {
        super(new StorageDataProviderConfig(config));
        this.producer = producer;
        this.liveDataTimeOut = (long)(config.liveDataTimeout * 1000);
    }


    @Override
    public boolean isEnabled()
    {
        return config.enabled && (producer.isEnabled() || storage.isEnabled());
    }


    @Override
    public SOSOfferingCapabilities generateCapabilities() throws ServiceException
    {
        SOSOfferingCapabilities capabilities = super.generateCapabilities();
        
        // enable real-time requests only if streaming data source is enabled
        if (producer.isEnabled())
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
        
        // enable real-time requests if streaming data source is enabled
        if (producer.isEnabled())
        {
            try
            {
                long now =  System.currentTimeMillis();
                
                // check latest record time
                long lastRecordTime = Long.MIN_VALUE;
                for (IStreamingDataInterface output: producer.getAllOutputs().values())
                {
                    // skip hidden outputs
                    if (config.hiddenOutputs != null && config.hiddenOutputs.contains(output.getName()))
                        continue;
                    
                    long recTime = output.getLatestRecordTime();
                    if (recTime != Long.MIN_VALUE && recTime > lastRecordTime)
                        lastRecordTime = recTime;
                }
                
                // if latest record is not too old, enable real-time
                if (now - lastRecordTime < liveDataTimeOut)
                    caps.getPhenomenonTime().setEndNow(true);
            }
            catch (SensorHubException e)
            {
                throw new ServiceException("Error while updating capabilities", e);
            }        
        }
    }
}
