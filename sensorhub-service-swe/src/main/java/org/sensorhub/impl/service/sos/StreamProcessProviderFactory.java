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

import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.processing.IStreamProcessModule;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.impl.SensorHub;


/**
 * <p>
 * Factory for stream processing data providers.<br/>
 * Most of the logic is inherited from {@link StreamDataProviderFactory}.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 28, 2015
 */
public class StreamProcessProviderFactory extends StreamDataProviderFactory<IStreamProcessModule<?>> implements ISOSDataProviderFactory, IEventListener
{
    
    
    protected StreamProcessProviderFactory(SOSServlet service, StreamProcessProviderConfig config) throws SensorHubException
    {
        super(service, config,
              (IStreamProcessModule<?>)SensorHub.getInstance().getModuleRegistry().getModuleById(config.processID),
              "Process");
    }

    
    @Override
    public ISOSDataProvider getNewDataProvider(SOSDataFilter filter) throws ServiceException
    {
        checkEnabled();
        return new StreamProcessDataProvider(producer, (StreamProcessProviderConfig)config, filter);
    }

}
