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
import java.util.Set;
import net.opengis.gml.v32.AbstractFeature;
import org.garret.perst.Index;
import org.garret.perst.Persistent;
import org.garret.perst.RectangleRn;
import org.garret.perst.SpatialIndexRn;
import org.garret.perst.Storage;
import org.sensorhub.api.persistence.IFeatureFilter;
import org.sensorhub.api.persistence.IFeatureStorage;
import org.vast.util.Bbox;
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
    
    
    public int getNumMatchingFeatures(IFeatureFilter filter)
    {
        return idIndex.size();
    }
    
    
    @Override
    public Bbox getFeaturesSpatialExtent()
    {
        RectangleRn boundingRect = geoIndex.getWrappingRectangle();
        if (boundingRect == null)
            return null;
        
        return PerstUtils.toBbox(boundingRect);
    }
    
    
    public Iterator<String> getFeatureIDs(IFeatureFilter filter)
    {
        // TODO optimize implementation to avoid loading whole feature objects
        
        final Iterator<AbstractFeature> it = getFeatures(filter);
        
        return new Iterator<String>()
        {
            public boolean hasNext()
            {
                return it.hasNext();
            }

            public String next()
            {
                return (String)it.next().getUniqueIdentifier();
            }

            public void remove()
            {   
            }            
        };
    }
    
    
    public Iterator<AbstractFeature> getFeatures(IFeatureFilter filter)
    {        
        // case of requesting by IDs
        Collection<String> foiIDs = filter.getFeatureIDs();
        if (foiIDs != null && !foiIDs.isEmpty())
        {
            final Set<String> ids = new LinkedHashSet<String>();
            ids.addAll(filter.getFeatureIDs());
            final Iterator<String> it = ids.iterator();
            
            Iterator<AbstractFeature> it2 = new Iterator<AbstractFeature>()
            {
                AbstractFeature nextFeature;
                
                public boolean hasNext()
                {
                    return (nextFeature != null);
                }

                public AbstractFeature next()
                {
                    AbstractFeature currentFeature = nextFeature;
                    nextFeature = null;
                    
                    while (nextFeature == null && it.hasNext())
                        nextFeature = idIndex.get(it.next());
                                        
                    return currentFeature;
                }

                public void remove()
                {                    
                }
            };
            
            it2.next();
            return it2;
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
