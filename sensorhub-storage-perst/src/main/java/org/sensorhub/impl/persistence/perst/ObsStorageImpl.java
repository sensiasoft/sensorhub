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

import java.util.Iterator;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.garret.perst.Storage;
import org.sensorhub.api.persistence.IFeatureFilter;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.StorageException;


/**
 * <p>
 * PERST implementation of {@link IObsStorage} for storing observations.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 15, 2015
 */
public class ObsStorageImpl extends BasicStorageImpl implements IObsStorage
{
        
    @Override
    protected BasicStorageRoot createRoot(Storage db)
    {
        ObsStorageRoot dbRoot = new ObsStorageRoot(db);
        return dbRoot;
    }
    
    
    @Override
    public final int getNumFois()
    {
        return ((ObsStorageRoot)dbRoot).getNumFois();
    }


    @Override
    public final Iterator<String> getFoiIDs()
    {
        return ((ObsStorageRoot)dbRoot).getFoiIDs();
    }


    @Override
    public final AbstractFeature getFoi(String uid)
    {
        return ((ObsStorageRoot)dbRoot).getFoi(uid);
    }


    @Override
    public Iterator<AbstractFeature> getFois(IFeatureFilter filter)
    {
        return ((ObsStorageRoot)dbRoot).getFois(filter);
    }
    
    
    @Override
    public synchronized void storeFoi(AbstractFeature foi)
    {
        ((ObsStorageRoot)dbRoot).storeFoi(foi);       
    }


    @Override
    public synchronized void addRecordType(String name, DataComponent recordStructure, DataEncoding recommendedEncoding) throws StorageException
    {
        dbRoot.addRecordType(name, recordStructure, recommendedEncoding);
        if (autoCommit)
            commit();
    }
}
