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
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.persistence.StorageConfig;
import org.sensorhub.impl.service.ogc.OGCServiceConfig;


/**
 * <p>
 * Configuration class for the SOS service module
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class SOSServiceConfig extends OGCServiceConfig
{
    
    /**
     * Set to true to enable transactional operation support
     */
    @DisplayInfo(desc="Set to true to enable transactional operation support")
    public boolean enableTransactional = false;
    
    
    /**
     * Storage configuration to use for newly registered sensors
     */
    @DisplayInfo(desc="Storage configuration to use for newly registered sensors")
    public StorageConfig newStorageConfig;
    
    
    /**
     * Configuration of data providers for SOS offerings
     */
    @DisplayInfo(label="Offerings", desc="Configuration of data providers for SOS offerings")
    public List<SOSProviderConfig> dataProviders = new ArrayList<SOSProviderConfig>();
    
    
    /**
     * Consumers configurations 
     */
    @DisplayInfo(desc="Configuration of data consumers for SOS offerings created by SOS-T")
    public List<SOSConsumerConfig> dataConsumers = new ArrayList<SOSConsumerConfig>();
}
