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
import org.vast.util.Bbox;


/**
 * <p>
 * Permission for filtering access to a specific geographic bounding region
 * of a dataset
 * </p>
 *
 * @author Alex Robin
 * @since Aug 22, 2016
 */
public class BboxPermission extends AbstractPermission implements IParameterizedPermission<Bbox>
{
    Bbox bbox = new Bbox();
    
    
    protected BboxPermission(AbstractPermission parent)
    {
        super(parent, "BBOX");
        setErrorMessage("Unallowed access to geographical region");
    }
    

    @Override
    public Bbox getValue()
    {
        return this.bbox;
    }
    

    @Override
    public void setValue(Bbox val)
    {
        this.bbox = val;        
    }
    
    
    @Override
    public boolean implies(IPermission perm)
    {
        if (!super.implies(perm))
            return false;
        
        if (!(perm instanceof BboxPermission))
            return false;
        
        if (!bbox.contains(((BboxPermission)perm).bbox))
            return false;
        
        return true;
    }
    

    @Override
    public IParameterizedPermission<Bbox> clone()
    {
        try
        {
            return (IParameterizedPermission<Bbox>)super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            return null;
        }
    }

}
