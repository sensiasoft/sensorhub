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
import java.util.Map;


@SuppressWarnings({ "serial", "rawtypes" })
public class MapProperty extends ContainerProperty
{
    
    public MapProperty(Object instance, Field f, MyBeanItemContainer container)
    {
        super(instance, f, container);
    }


    @Override
    public void setValue(MyBeanItemContainer newValue) throws ReadOnlyException
    {
        try
        {
            Map map = (Map)f.get(instance);
            map.clear();
            
            for (Object itemId: container.getItemIds())
            {
                Object bean = container.getUnfilteredItem(itemId).getBean();
                map.put((String)itemId, bean);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error while updating collection");
        }
    }
}