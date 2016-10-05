/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.security;

import org.sensorhub.api.security.IParameterizedPermission;
import org.sensorhub.api.security.IPermission;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Permission for filtering access to a specific time range of a dataset
 * </p>
 *
 * @author Alex Robin
 * @since Aug 22, 2016
 */
public class TimeRangePermission extends AbstractPermission implements IParameterizedPermission<TimeExtent>
{
    TimeExtent timeExtent = new TimeExtent();
    
    
    protected TimeRangePermission(AbstractPermission parent)
    {
        super(parent, "TIME");
        setErrorMessage("Unallowed access to time extent");
    }
    

    @Override
    public TimeExtent getValue()
    {
        return this.timeExtent;
    }
    

    @Override
    public void setValue(TimeExtent val)
    {
        this.timeExtent = val;        
    }
    
    
    @Override
    public boolean implies(IPermission perm)
    {
        if (!super.implies(perm))
            return false;
        
        if (!(perm instanceof TimeRangePermission))
            return false;
        
        if (!timeExtent.contains(((TimeRangePermission)perm).timeExtent))
            return false;
        
        return true;
    }
    

    @Override
    public IParameterizedPermission<TimeExtent> clone()
    {
        try
        {
            return (IParameterizedPermission<TimeExtent>)super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            return null;
        }
    }
}
