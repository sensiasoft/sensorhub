/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import java.util.List;
import java.util.Map;
import net.opengis.sensorml.v20.AbstractProcess;
import org.garret.perst.Persistent;
import org.sensorhub.api.persistence.IMultiSourceStorage;
import org.sensorhub.api.persistence.StorageException;


/**
 * <p>
 * PERST implementation of multisource storage
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since May 8, 2015
 */
public class MultiSourceStorageImpl extends Persistent implements IMultiSourceStorage
{
    Map<String, ObsStorageRoot> obsStores;
    FeatureStoreImpl featureStore;
    
    
    @Override
    public AbstractProcess getDataSourceDescription(String uid)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public List<AbstractProcess> getDataSourceDescriptionHistory(String uid, double startTime, double endTime)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public AbstractProcess getDataSourceDescriptionAtTime(String uid, double time)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void storeDataSourceDescription(AbstractProcess process) throws StorageException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void updateDataSourceDescription(AbstractProcess process) throws StorageException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void removeDataSourceDescription(String uid, double time)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void removeDataSourceDescriptionHistory(String uid, double startTime, double endTime)
    {
        // TODO Auto-generated method stub

    }

}
