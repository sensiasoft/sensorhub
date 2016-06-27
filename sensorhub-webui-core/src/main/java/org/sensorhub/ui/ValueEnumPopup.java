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

import java.util.Arrays;
import org.sensorhub.ui.ValueEntryPopup.ValueCallback;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


public class ValueEnumPopup extends Window
{
    private static final long serialVersionUID = 3541095366722509161L;

    
    @SuppressWarnings({ "serial", "rawtypes" })
    public ValueEnumPopup(int width, final ValueCallback callback, final Enum[] allowedValues)
    {
        super("New Value");
        VerticalLayout layout = new VerticalLayout();
        
        final ListSelect listBox = new ListSelect();
        listBox.setNullSelectionAllowed(false);
        listBox.setWidth(width, Unit.PIXELS);
        listBox.setRows(10);
        listBox.addItems(Arrays.asList(allowedValues));
        layout.addComponent(listBox);
        
        listBox.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event)
            {
                ValueEnumPopup.this.close();
                callback.newValue(event.getProperty().getValue());
            }
        });
        
        setContent(layout);
        center();
    }
}
