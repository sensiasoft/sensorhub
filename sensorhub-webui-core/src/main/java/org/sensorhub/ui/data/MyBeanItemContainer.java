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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.vaadin.data.Property;
import com.vaadin.data.util.AbstractInMemoryContainer;


@SuppressWarnings("serial")
public class MyBeanItemContainer<BeanType> extends AbstractInMemoryContainer<Object, Object, MyBeanItem<BeanType>>
{
    final Map<Object, MyBeanItem<BeanType>> itemIdToItem = new HashMap<Object, MyBeanItem<BeanType>>();
    final MyBeanItem<BeanType> templateItem;
    
    
    public MyBeanItemContainer(Class<BeanType> beanType) throws IllegalArgumentException
    {
        try
        {
            templateItem = new MyBeanItem<BeanType>(beanType.newInstance());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    
    public MyBeanItem<BeanType> addBean(BeanType bean)
    {
        MyBeanItem<BeanType> newItem = new MyBeanItem<BeanType>(bean);
        Integer newItemId = (Integer)bean.hashCode();
        internalAddItemAtEnd(newItemId, newItem, false);
        fireItemAdded(indexOfId(newItem), newItemId, newItem);
        return newItem;
    }


    @Override
    public Collection<?> getContainerPropertyIds()
    {
        if (!itemIdToItem.isEmpty())
            return itemIdToItem.values().iterator().next().getItemPropertyIds();
       
        if (templateItem != null)
            return templateItem.getItemPropertyIds();
            
        return Collections.EMPTY_LIST;
    }


    @Override
    public Property<?> getContainerProperty(Object itemId, Object propertyId)
    {
        return super.getItem(itemId).getItemProperty(propertyId);
    }


    @Override
    public Class<?> getType(Object propertyId)
    {
        if (!itemIdToItem.isEmpty())
            return itemIdToItem.values().iterator().next().getItemProperty(propertyId).getType();
                    
        if (templateItem != null)
            return templateItem.getItemProperty(propertyId).getType();
        
        return null;
    }

    
    @Override
    protected void registerNewItem(int position, Object itemId, MyBeanItem<BeanType> item)
    {
        itemIdToItem.put(itemId, item);
    }
    
    
    @Override
    protected MyBeanItem<BeanType> getUnfilteredItem(Object itemId)
    {
        return itemIdToItem.get(itemId);
    }


    @Override
    public boolean removeItem(Object itemId) throws UnsupportedOperationException
    {
        boolean ret = internalRemoveItem(itemId);
        itemIdToItem.remove(itemId);
        fireItemRemoved(indexOfId(itemId), itemId);
        return ret;
    }


    @Override
    public boolean removeAllItems() throws UnsupportedOperationException
    {
        super.internalRemoveAllItems();
        itemIdToItem.clear();
        this.fireItemsRemoved(0, firstItemId(), size());
        return true;
    }
    
}
