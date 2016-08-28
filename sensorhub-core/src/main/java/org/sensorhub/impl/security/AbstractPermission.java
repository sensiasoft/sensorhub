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

import java.util.HashMap;
import java.util.Map;
import org.sensorhub.api.security.IPermission;


/**
 * <p>
 * Base class for all permissions
 * </p>
 *
 * @author Alex Robin
 * @since Aug 23, 2016
 */
public abstract class AbstractPermission implements IPermission
{
    protected String name;
    protected IPermission parent;
    protected Map<String, IPermission> children;
    protected String errorMsg;
    
    
    protected AbstractPermission(IPermission parent, String name, String errorMsg)
    {
        this.parent = parent;
        if (parent != null)
            parent.getChildren().put(name, this);
        this.name = name;
        this.errorMsg = errorMsg;
    }
    
    
    @Override
    public IPermission getParent()
    {
        return this.parent;
    }
    
    
    @Override
    public Map<String, IPermission> getChildren()
    {
        if (children == null)
            children = new HashMap<String, IPermission>();
        
        return children;
    }
    
    
    @Override
    public boolean hasChildren()
    {
        return (children != null && !children.isEmpty());
    }
    
    
    @Override
    public String getName()
    {
        return this.name;
    }
    
    
    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(name);
        IPermission perm = this;
        while ((perm = perm.getParent()) != null)
            buf.insert(0, perm.getName() + ":");
        return buf.toString();
    }
    
    
    @Override
    public int hashCode()
    {
        return name.hashCode();
    }
    
    
    @Override
    public String getErrorMessage()
    {
        if (errorMsg != null)
            return this.errorMsg;
        else
            return "Permission denied: " + toString();
    }


    @Override
    public boolean implies(IPermission perm)
    {
        if (!name.equals(perm.getName()))
            return false;
        
        return true;
    }
}
