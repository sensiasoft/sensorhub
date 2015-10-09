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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;


/**
 * <p>
 * PERST implementation of FoI observation periods
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since May 8, 2015
 */
class FoiTimesStoreImpl extends Persistent
{
    class FeatureEntry
    {
        String uid;
        List<double[]> timePeriods = new ArrayList<double[]>();
        
        FeatureEntry(String uid)
        {
            this.uid = uid;
        }
    }
    
    
    class FoiTimePeriod
    {
        String uid;
        double[] timePeriod;
        
        FoiTimePeriod(String uid, double[] timePeriod)
        {
            this.uid = uid;
            this.timePeriod = timePeriod;
        }
    }
    
    
    class FoiTimePeriodComparator implements Comparator<FoiTimePeriod>
    {
        public int compare(FoiTimePeriod p0, FoiTimePeriod p1)
        {
            return (int)Math.signum(p0.timePeriod[0] - p1.timePeriod[0]);
        }        
    }
        
    
    Map<String, FeatureEntry> idIndex;
    transient String lastFoi;
    
    
    // default constructor needed by PERST on Android JVM
    FoiTimesStoreImpl() {}

    
    FoiTimesStoreImpl(Storage db)
    {
        super(db);
        idIndex = db.createMap(String.class);
    }
    
    
    Set<FoiTimePeriod> getSortedFoiTimes(Collection<String> uids)
    {
        // create set with custom comparator for sorting FoiTimePeriod objects
        TreeSet<FoiTimePeriod> foiTimes = new TreeSet<FoiTimePeriod>(new FoiTimePeriodComparator());
        
        // TODO handle case of overlaping FOI periods?
        
        if (uids != null && uids.size() > 0)
        {
            for (String uid: uids)
            {
                FeatureEntry fEntry = idIndex.get(uid);
                if (fEntry == null)
                    continue;
                
                // add each period to sorted set
                for (double[] timePeriod: fEntry.timePeriods)
                    foiTimes.add(new FoiTimePeriod(uid, timePeriod));
            }
        }
        else // no filtering on FOI ID -> select them all
        {
            for (FeatureEntry fEntry: idIndex.values())
            {
                String uid = fEntry.uid;
                
                // add each period to sorted set
                for (double[] timePeriod: fEntry.timePeriods)
                    foiTimes.add(new FoiTimePeriod(uid, timePeriod));
            }
        }
        
        return foiTimes;
    }
    
    
    void updateFoiPeriod(String uid, double timeStamp)
    {
        // if lastFoi is null (after restart), set to the one for which we last received data
        if (lastFoi == null)
        {
            double latestTime = Double.NEGATIVE_INFINITY;
            for (FeatureEntry entry: idIndex.values())
            {
                int nPeriods = entry.timePeriods.size();
                if (entry.timePeriods.get(nPeriods-1)[1] > latestTime)
                    lastFoi = entry.uid;
            }
        }
        
        FeatureEntry entry = idIndex.get(uid);
        if (entry == null)
        {
            entry = new FeatureEntry(uid);
            idIndex.put(uid, entry);
        }
        
        // if same foi, keep growing period
        if (uid.equals(lastFoi))
        {
            int numPeriods = entry.timePeriods.size();
            entry.timePeriods.get(numPeriods-1)[1] = timeStamp;
        }
        
        // otherwise start new period
        else
            entry.timePeriods.add(new double[] {timeStamp, timeStamp});
        
        // mark entry as modified so changes can be commited
        getStorage().modify(entry);
        
        // remember current FOI
        lastFoi = uid;
    }
    
    
    void remove(String uid)
    {
        idIndex.remove(uid);
    }    
}
