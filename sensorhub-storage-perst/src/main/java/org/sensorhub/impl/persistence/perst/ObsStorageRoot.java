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
 * PERST implementation of an observation storage fed by a single producer.<br/>
 * This adds information about feature of interests observation times.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since May 8, 2015
 */
class ObsStorageRoot extends BasicStorageRoot implements IObsStorage
{
    FeatureStoreImpl featureStore;
    
    
    // default constructor needed on Android JVM
    ObsStorageRoot() {}
    
    
    public ObsStorageRoot(Storage db)
    {
        super(db);
        this.featureStore = new FeatureStoreImpl(db);
    }
    
    
    @Override
    public int getNumFois()
    {
        return featureStore.getNumFeatures();
    }


    @Override
    public Iterator<String> getFoiIDs()
    {
        return featureStore.getFeatureIDs();
    }


    @Override
    public AbstractFeature getFoi(String uid)
    {
        return featureStore.getFeatureById(uid);
    }


    @Override
    public Iterator<AbstractFeature> getFois(IFeatureFilter filter)
    {
        return featureStore.getFeatureIterator(filter);
    }
    
    
    @Override
    public void storeFoi(AbstractFeature foi)
    {
        featureStore.store(foi);
    }


    @Override
    public void addRecordType(String name, DataComponent recordStructure, DataEncoding recommendedEncoding) throws StorageException
    {
        ObsSeriesImpl newTimeSeries = new ObsSeriesImpl(getStorage(), recordStructure, recommendedEncoding);
        dataStores.put(name, newTimeSeries);
        modify();
    }
}
