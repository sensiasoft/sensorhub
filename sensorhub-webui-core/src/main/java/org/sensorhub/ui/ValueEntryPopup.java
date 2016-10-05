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

import java.util.Collection;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


public class ValueEntryPopup extends Window
{
    private static final long serialVersionUID = 9099071384769283253L;


    protected interface ValueCallback
    {
        public void newValue(Object value);
    }
    
    
    @SuppressWarnings("serial")
    public ValueEntryPopup(int width, final ValueCallback callback, final Collection<?> possibleValues)
    {
        super("Enter New Value");
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        
        Field<?> input = null;
        if (possibleValues != null && !possibleValues.isEmpty())
        {
            ComboBox select = new ComboBox();
            select.setWidth(width, Unit.PIXELS);
            select.setNullSelectionAllowed(false);
            
            for (Object val: possibleValues)
            {
                // when null is included in the list it means we also
                // accept new values entered by the user
                if (val == null)
                {
                    select.setNewItemsAllowed(true);
                    select.setImmediate(true);
                }
                else
                    select.addItem(val);
            }
            
            layout.addComponent(select);
            select.focus();
            input = select;
        }
        else
        {
            TextField text = new TextField();
            text.setWidth(width, Unit.PIXELS);
            layout.addComponent(text);
            text.focus();
            input = text;
        }
        
        input.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event)
            {
                ValueEntryPopup.this.close();
                callback.newValue(event.getProperty().getValue());
            }
        });
        
        setContent(layout);
        center();
    }
}
