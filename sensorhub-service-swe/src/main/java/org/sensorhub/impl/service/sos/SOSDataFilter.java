/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Initial Developer of the Original Code is SENSIA SOFTWARE LLC.
 Portions created by the Initial Developer are Copyright (C) 2012
 the Initial Developer. All Rights Reserved.

 Please Contact Alexandre Robin <alex.robin@sensiasoftware.com> for more
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.vast.util.TimeExtent;
import com.vividsolutions.jts.geom.Polygon;


/**
 * <p>
 * Filter to be used with SOS data providers
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @date Nov 25, 2012
 * */
public class SOSDataFilter
{
    Set<String> observables = new LinkedHashSet<String>();
    Set<String> foiIds = new LinkedHashSet<String>();
    TimeExtent timeRange = new TimeExtent(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    Polygon roi;
    
    double replaySpeedFactor = Double.NaN;
    long maxObsCount = Long.MAX_VALUE;
    
    
    public SOSDataFilter(TimeExtent timeRange)
    {
        this(null, null, timeRange);
    }
    
    
    public SOSDataFilter(String observable)
    {
        this.observables.add(observable);
    }
    
    
    public SOSDataFilter(String observable, TimeExtent timeRange)
    {
        this.observables.add(observable);
        this.timeRange = timeRange.copy();
    }
    
    
    public SOSDataFilter(List<String> foiIds, List<String> observables, TimeExtent timeRange)
    {
        if (foiIds != null)
            this.foiIds.addAll(foiIds);
        
        if (observables != null)
            this.observables.addAll(observables);
        
        if (timeRange != null)
            this.timeRange = timeRange.copy();
    }
    

    public Set<String> getObservables()
    {
        return observables;
    }
    
    
    public Set<String> getFoiIds()
    {
        return foiIds;
    }


    public TimeExtent getTimeRange()
    {
        return timeRange;
    }


    public Polygon getRoi()
    {
        return roi;
    }


    public void setRoi(Polygon roi)
    {
        this.roi = roi;
    }


    public double getReplaySpeedFactor()
    {
        return replaySpeedFactor;
    }


    public void setReplaySpeedFactor(double replaySpeedFactor)
    {
        this.replaySpeedFactor = replaySpeedFactor;
    }


    public long getMaxObsCount()
    {
        return maxObsCount;
    }


    public void setMaxObsCount(long maxObsCount)
    {
        this.maxObsCount = maxObsCount;
    }
}
