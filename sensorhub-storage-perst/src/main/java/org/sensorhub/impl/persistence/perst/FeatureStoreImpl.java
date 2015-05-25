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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.opengis.gml.v32.AbstractFeature;
import org.garret.perst.Index;
import org.garret.perst.Persistent;
import org.garret.perst.RectangleRn;
import org.garret.perst.SpatialIndexRn;
import org.garret.perst.Storage;
import org.sensorhub.api.persistence.IFeatureFilter;
import org.sensorhub.api.persistence.IFeatureStorage;
import com.vividsolutions.jts.geom.Envelope;


/**
 * <p>
 * PERST implementation of FoI store with indexes and search methods
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since May 8, 2015
 */
class FeatureStoreImpl extends Persistent implements IFeatureStorage
{
    Index<AbstractFeature> idIndex;
    SpatialIndexRn<AbstractFeature> geoIndex;
    
    
    // default constructor needed on Android JVM
    FeatureStoreImpl() {}

    
    FeatureStoreImpl(Storage db)
    {
        idIndex = db.createIndex(String.class, true);
        geoIndex = db.createSpatialIndexRn();
    }
    
    
    public int getNumFeatures()
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
    
    
    public AbstractFeature getFeatureById(String uid)
    {
        return idIndex.get(uid);
    }
    
    
    public Iterator<AbstractFeature> getFeatureIterator(IFeatureFilter filter)
    {        
        // case of requesting several IDs at once
        Collection<String> foiIDs = filter.getFeatureIDs();
        if (foiIDs != null && !foiIDs.isEmpty())
        {
            final Set<String> ids = new LinkedHashSet<String>();
            ids.addAll(filter.getFeatureIDs());
            final Iterator<String> it = ids.iterator();
            
            return new Iterator<AbstractFeature>()
            {
                int count = 0;
                
                public boolean hasNext()
                {
                    return count < ids.size();
                }

                public AbstractFeature next()
                {
                    count++;
                    return idIndex.get(it.next());
                }

                public void remove()
                {                    
                }
            };
        }
            
        // case of ROI
        if (filter.getRoi() != null)
        {
            // iterate through spatial index using bounding rectangle
            // TODO filter on exact polygon geometry using JTS
            Envelope env = filter.getRoi().getEnvelopeInternal();
            double[] coords = new double[] {env.getMinX(), env.getMinY(), Double.NEGATIVE_INFINITY, env.getMaxX(), env.getMaxY(), Double.POSITIVE_INFINITY};
            return geoIndex.iterator(new RectangleRn(coords));
        }
        
        // TODO handle ROI + IDs?        
        
        return idIndex.iterator();
    }
    
    
    void store(AbstractFeature foi)
    {
        boolean newFoi = idIndex.put(foi.getUniqueIdentifier(), foi);
        
        if (newFoi && foi.getLocation() != null)
        {
            RectangleRn rect = PerstUtils.getBoundingRectangle(foi.getLocation());
            geoIndex.put(rect, foi);
        }
    }    
}
