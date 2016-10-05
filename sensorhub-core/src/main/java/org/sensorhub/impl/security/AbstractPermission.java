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

import java.util.LinkedHashMap;
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
    protected String name, label, description;
    protected IPermission parent;
    protected Map<String, IPermission> children;
    protected String errorMsg;
    
    
    protected AbstractPermission()
    {        
    }
    
    
    public AbstractPermission(IPermission parent, String name)
    {
        this(parent, name, null, null);
    }
    
    
    public AbstractPermission(IPermission parent, String name, String label, String description)
    {
        this.parent = parent;
        if (parent != null)
            parent.getChildren().put(name, this);
        this.name = name;
        this.label = label;
        this.description = description;
    }
    
    
    @Override
    public String getName()
    {
        return this.name;
    }
    
    
    public String getLabel()
    {
        if (label != null)
            return label;
        
        return name;
    }


    public String getDescription()
    {
        return description;
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
            children = new LinkedHashMap<String, IPermission>();
        
        return children;
    }
    
    
    @Override
    public boolean hasChildren()
    {
        return (children != null && !children.isEmpty());
    }


    @Override
    public String getFullName()
    {
        StringBuilder buf = new StringBuilder(name);
        IPermission perm = this;
        while ((perm = perm.getParent()) != null)
            buf.insert(0, perm.getName() + "/");
        return buf.toString();
    }
    
    
    @Override
    public String toString()
    {
        return (label != null) ? label : name;
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
            return "Permission denied: " + getFullName();
    }
    
    
    public void setErrorMessage(String msg)
    {
        this.errorMsg = msg;
    }


    @Override
    public boolean implies(IPermission perm)
    {
        if (!name.equals(perm.getName()))
            return false;
        
        return true;
    }
}
