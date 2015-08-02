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
import org.sensorhub.api.config.DisplayInfo;


@SuppressWarnings("serial")
public class ComplexProperty extends BaseProperty<MyBeanItem<Object>>
{
    Object parentObj;
    MyBeanItem<Object> item;
    

    public ComplexProperty(Object parentObj, Field f, MyBeanItem<Object> item)
    {
        this.parentObj = parentObj;
        this.f = f;
        this.item = item;
    }


    @Override
    public MyBeanItem<Object> getValue()
    {
        return item;
    }


    @Override
    public void setValue(MyBeanItem<Object> newValue) throws ReadOnlyException
    {
        try
        {
            this.item = newValue;
            f.set(parentObj, newValue.getBean());
            fireValueChange();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Cannot set object field " + f.getName());
        }
        
    }


    @Override
    public Class<? extends MyBeanItem<Object>> getType()
    {
        return (Class<? extends MyBeanItem<Object>>)item.getClass();
    }
    
    
    public String getLabel()
    {
        DisplayInfo ann = f.getAnnotation(DisplayInfo.class);
        if (ann != null)
            return ann.label();
        else
            return null;
    }
    
    
    public Class<?> getBeanType()
    {
        return f.getType();
    }
}