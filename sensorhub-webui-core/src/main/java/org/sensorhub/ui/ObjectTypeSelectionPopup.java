/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.sensorhub.ui.api.UIConstants;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnHeaderMode;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;


public class ObjectTypeSelectionPopup extends Window
{
    private static final long serialVersionUID = 280657033210669136L;


    protected interface ObjectTypeSelectionCallback
    {
        public void typeSelected(Class<?> clazz);
    }
    
    
    public ObjectTypeSelectionPopup(String title, final Map<String, Class<?>> typeList, final ObjectTypeSelectionCallback callback)
    {
        super(title);
        VerticalLayout layout = new VerticalLayout();
        
        // generate table with type list
        final Table table = new Table();
        table.setSizeFull();
        table.setSelectable(true);
        table.setColumnReorderingAllowed(true);        
        table.addContainerProperty(UIConstants.PROP_NAME, String.class, null);
        table.setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        table.setPageLength(10);
        table.setMultiSelect(false);
        
        final Map<Object, Class<?>> idTypeMap = new HashMap<Object, Class<?>>();
        for (Entry<String, Class<?>> item: typeList.entrySet())
        {
            Object id = table.addItem(new Object[] {item.getKey()}, null);
            idTypeMap.put(id, item.getValue());
        }
        layout.addComponent(table);
        
        // add OK button
        Button okButton = new Button("OK");
        okButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event)
            {
                Object selectedItemId = table.getValue();
                
                if (selectedItemId != null)
                {
                    Class<?> clazz = idTypeMap.get(selectedItemId);
                    if (clazz != null)
                        callback.typeSelected(clazz);
                }
                
                close();
            }
        });
        layout.addComponent(okButton);
        layout.setComponentAlignment(okButton, Alignment.MIDDLE_CENTER);
        
        setContent(layout);
        center();
    }
}
