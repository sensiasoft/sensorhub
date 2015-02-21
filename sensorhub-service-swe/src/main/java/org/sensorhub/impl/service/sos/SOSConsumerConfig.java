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
import org.vast.ows.sos.ISOSDataConsumer;


public abstract class SOSConsumerConfig
{

    /**
     * Flag set if consumer is enabled, unset if disabled
     */
    public boolean enabled;
    
    
    /**
     * Offering URI
     */
    public String offering;
    
    
    /**
     * @return an instance of data consumer corresponding to this config
     */
    protected abstract ISOSDataConsumer getConsumerInstance() throws SensorHubException;
}