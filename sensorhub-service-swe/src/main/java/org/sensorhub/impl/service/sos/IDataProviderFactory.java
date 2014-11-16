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

import net.opengis.sensorml.v20.AbstractProcess;
import org.vast.ows.sos.ISOSDataProviderFactory;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.util.DateTime;


public interface IDataProviderFactory extends ISOSDataProviderFactory
{

    /**
     * Allows to check if provider is enabled
     * @return
     */
    public boolean isEnabled();
    
    
    /**
     * Builds the offering capabilities using the provider configuration
     * This will connect to source providers to retrieve the necessary metadata
     * @return SOS capabilities object containing the maximum of metadata
     */
    public SOSOfferingCapabilities generateCapabilities() throws Exception;
        
    
    /**
     * Retrieves the SensorML description associated to this data source
     * @return
     */
    public AbstractProcess generateSensorMLDescription(DateTime t) throws Exception;
    
    
    /**
     * Called when the provider is removed
     */    
    public void cleanup();

}