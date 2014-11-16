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
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.util.DateTime;


/**
 * <p>
 * Factory for storage data providers.
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
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 15, 2013
 */
public class StorageDataProviderFactory implements IDataProviderFactory
{
    StorageDataProviderConfig config;
    
    
    protected StorageDataProviderFactory(StorageDataProviderConfig config)
    {
        this.config = config;
    }
    
    
    @Override
    public SOSOfferingCapabilities generateCapabilities()
    {
        SOSOfferingCapabilities offering = new SOSOfferingCapabilities();
        
        return offering;
    }
    
    
    @Override
    public ISOSDataProvider getNewProvider(SOSDataFilter filter) throws Exception
    {
        
        return null;
    }


    @Override
    public AbstractProcess generateSensorMLDescription(DateTime t)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean isEnabled()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public void cleanup()
    {
        // TODO Auto-generated method stub
        
    }

}
