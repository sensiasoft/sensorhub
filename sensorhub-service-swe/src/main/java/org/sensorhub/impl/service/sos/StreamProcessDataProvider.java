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
import org.sensorhub.api.processing.IStreamProcessModule;
import org.sensorhub.api.service.ServiceException;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.ows.sos.SOSDataFilter;


/**
 * <p>
 * Implementation of SOS data provider connecting to a process via 
 * SensorHub's stream processing API.<br/>
 * Most of the logic is inherited from {@link StreamDataProvider}.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 28, 2015
 */
public class StreamProcessDataProvider extends StreamDataProvider implements ISOSDataProvider, IEventListener
{
    
    public StreamProcessDataProvider(IStreamProcessModule<?> srcProcess, StreamProcessProviderConfig config, SOSDataFilter filter) throws ServiceException
    {
        super(srcProcess, config, filter);
    }
}
