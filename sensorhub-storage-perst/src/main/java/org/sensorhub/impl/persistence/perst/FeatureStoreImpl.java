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
import java.util.Map;
import net.opengis.gml.v32.AbstractFeature;
import org.garret.perst.Index;
import org.garret.perst.Persistent;
import org.garret.perst.SpatialIndexR2;
import org.garret.perst.Storage;
import org.sensorhub.api.persistence.IObsFilter;


/**
 * <p>
 * PERST implementation of FoI store with indexes and search methods
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since May 8, 2015
 */
class FeatureStoreImpl extends Persistent
{
    Index<AbstractFeature> idIndex;
    SpatialIndexR2<AbstractFeature> geoIndex;
    
    
    // default constructor needed on Android JVM
    FeatureStoreImpl() {}

    
    FeatureStoreImpl(Storage db)
    {
        idIndex = db.createIndex(String.class, true);
        geoIndex = db.createSpatialIndexR2();
    }
    
    
    int getNumFeatures()
    {
        return idIndex.size();
    }
    
    
    Iterator<String> getFeatureIDs()
    {
        final Iterator<Map.Entry<Object,AbstractFeature>> entryIt = idIndex.entryIterator();
        
        return new Iterator<String>()
        {
            public boolean hasNext()
            {
                return entryIt.hasNext();
            }

            public String next()
            {
                return (String)entryIt.next().getKey();
            }

            public void remove()
            {   
            }            
        };
    }
    
    
    AbstractFeature getFeatureById(String uid)
    {
        return idIndex.get(uid);
    }
    
    
    Iterator<AbstractFeature> getFois(IObsFilter filter)
    {
        return idIndex.iterator();
        
        // TODO iterate through spatial index using bounding rectangle
        // + filter on exact polygon geometry using JTS
        
        // TODO handle case of requesting several IDs at once
        
    }
    
    
    void store(AbstractFeature foi)
    {
        idIndex.put(foi.getUniqueIdentifier(), foi);
    }    
}
