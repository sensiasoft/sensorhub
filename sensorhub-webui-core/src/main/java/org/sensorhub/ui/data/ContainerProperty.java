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
import java.util.Collection;


@SuppressWarnings({ "serial", "rawtypes" })
public class ContainerProperty extends BaseProperty<MyBeanItemContainer>
{
    Object instance;
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
        try
        {
            Collection list = (Collection)f.get(instance);
            list.clear();
            
            for (Object itemId: container.getItemIds())
            {
                Object bean = container.getUnfilteredItem(itemId).getBean();
                list.add(bean);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error while updating collection");
        }
    }


    @Override
    public Class<? extends MyBeanItemContainer> getType()
    {
        return container.getClass();
    }


    public Field getField()
    {
        return f;
    }
}