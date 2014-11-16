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


/**
 * <p>
 * Configuration class for SOS data providers using the persistence API
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 14, 2013
 */
public class StorageDataProviderConfig extends SOSProviderConfig
{
    
    /**
     * Local ID of storage to use as data source
     */
    public String storageID;
    
    
    /**
     * IDs of producers whose data will be exposed through the SOS
     * If this is null, all producers offered by storage are exposed
     */
    public String[] producerIDs;
    

    @Override
    protected IDataProviderFactory getFactory()
    {
        return new StorageDataProviderFactory(this);
    }
}
