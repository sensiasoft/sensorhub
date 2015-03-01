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

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * Configuration class for SOS data providers using the streaming data API.
 * A storage can also be associated to the provider so that archive requests
 * can be handled through the same offering.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 20, 2015
 */
public abstract class StreamDataProviderConfig extends SOSProviderConfig
{
    
    /**
     * Local ID of storage containing the process data
     * to use as data source for archive requests
     */
    public String storageID;
    
    
    /**
     * Names of process outputs to hide from SOS
     */
    public List<String> hiddenOutputs = new ArrayList<String>();
    
    
    /**
     * If true, forward "new data" events via the WS-Notification
     * interface of the service
     */
    public boolean activateNotifications;
    
    
    /**
     * Time-out after which real-time requests are disabled if no more
     * measurements are received. Real-time is reactivated as soon as
     * new records start being received again.
     */
    public double liveDataTimeout = 10.0;

}
