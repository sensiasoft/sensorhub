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

import java.util.Iterator;
import net.opengis.gml.v32.AbstractFeature;
import org.sensorhub.api.persistence.IFoiFilter;
import org.vast.ogc.gml.JTSUtils;
import com.vividsolutions.jts.geom.Geometry;


public class FilteredFoiIterator implements Iterator<AbstractFeature>
{
    AbstractFeature nextFeature;
    Iterator<? extends AbstractFeature> allFois;
    IFoiFilter filter;


    public FilteredFoiIterator(Iterator<? extends AbstractFeature> allFois, IFoiFilter filter)
    {
        this.allFois = allFois;
        this.filter = filter;
    }


    public boolean hasNext()
    {
        while (allFois.hasNext())
        {
            AbstractFeature f = allFois.next();
            boolean keep = true;

            if (filter != null)
            {
                // filter on feature ID
                if (filter.getFeatureIDs() != null && !filter.getFeatureIDs().isEmpty())
                {
                    if (!filter.getFeatureIDs().contains(f.getUniqueIdentifier()))
                        keep = false;
                }
    
                // filter on feature geometry
                if (keep && filter.getRoi() != null && f.getLocation() != null)
                {
                    Geometry fGeom = JTSUtils.getAsJTSGeometry(f.getLocation());
                    if (fGeom.disjoint(filter.getRoi()))
                        keep = false;
                }
            }

            if (keep)
            {
                nextFeature = f;
                break;
            }
        }

        return false;
    }


    public AbstractFeature next()
    {
        return nextFeature;
    }


    public void remove()
    {
    }
}
