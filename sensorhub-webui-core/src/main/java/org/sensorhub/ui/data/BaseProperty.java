/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui.data;

import java.lang.reflect.Field;
import org.sensorhub.api.comm.ICommNetwork.NetworkType;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.AddressType;
import org.sensorhub.api.config.DisplayInfo.FieldType;
import org.sensorhub.api.config.DisplayInfo.FieldType.Type;
import org.sensorhub.api.config.DisplayInfo.ModuleType;
import org.sensorhub.api.config.DisplayInfo.TextInfo;
import org.sensorhub.api.config.DisplayInfo.ValueRange;
import org.sensorhub.api.module.IModule;
import com.vaadin.data.util.AbstractProperty;


@SuppressWarnings({ "serial" })
public abstract class BaseProperty<T> extends AbstractProperty<T>
{
    protected Field f;
    
    
    public BaseProperty()
    {
        super();
    }

    
    public String getLabel()
    {
        DisplayInfo ann = f.getAnnotation(DisplayInfo.class);
        if (ann != null && ann.label().length() > 0)
            return ann.label();
        else
            return null;
    }

    
    public String getDescription()
    {
        DisplayInfo ann = f.getAnnotation(DisplayInfo.class);
        if (ann != null && ann.desc().length() > 0)
            return ann.desc();
        else
            return null;
    }
    
    
    public ValueRange getValueRange()
    {
        return f.getAnnotation(ValueRange.class);
    }
    
    
    public TextInfo getTextInfo()
    {
        return f.getAnnotation(TextInfo.class);
    }
    
    
    public Type getFieldType()
    {
        FieldType ann = f.getAnnotation(FieldType.class);
        if (ann != null)
            return ann.value();
        return null;
    }
    
    
    @SuppressWarnings("rawtypes")
    public Class<? extends IModule> getModuleType()
    {
        ModuleType ann = f.getAnnotation(ModuleType.class);
        if (ann != null)
            return ann.value();
        return null;
    }
    
    
    public NetworkType getAddressType()
    {
        AddressType ann = f.getAnnotation(AddressType.class);
        if (ann != null)
            return ann.value();
        return null;
    }
}