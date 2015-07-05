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
import java.util.List;
import org.sensorhub.api.config.DisplayInfo;
import com.vaadin.data.util.AbstractProperty;


@SuppressWarnings({ "serial", "rawtypes" })
public class ContainerProperty extends AbstractProperty<MyBeanItemContainer>
{
    Object instance;
    Field f;
    MyBeanItemContainer container;
    

    public ContainerProperty(Object instance, Field f, MyBeanItemContainer container)
    {
        this.instance = instance;
        this.f = f;
        this.container = container;
    }


    @Override
    public MyBeanItemContainer getValue()
    {
        return container;
    }


    @Override
    public void setValue(MyBeanItemContainer newValue) throws ReadOnlyException
    {
        if (List.class.isAssignableFrom(f.getType()))
        {
            try
            {
                List list = (List)f.get(instance);
                list.clear();
                
                for (Object itemId: container.getItemIds())
                {
                    Object bean = container.getUnfilteredItem(itemId).getBean();
                    list.add(bean);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }


    @Override
    public Class<? extends MyBeanItemContainer> getType()
    {
        return container.getClass();
    }
    
    
    public String getLabel()
    {
        DisplayInfo ann = f.getAnnotation(DisplayInfo.class);
        if (ann != null)
            return ann.label();
        else
            return null;
    }


    public Field getField()
    {
        return f;
    }
}