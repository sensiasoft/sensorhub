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

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.ui.api.IModuleConfigFormBuilder;
import org.sensorhub.ui.data.ContainerProperty;
import org.sensorhub.ui.data.FieldProperty;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Button.ClickEvent;


/**
 * <p>
 * Generic form builder based on Vaadin framework
 * This auto-generates widget giving types of properties. 
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 1, 2014
 */
public class GenericConfigFormBuilder implements IModuleConfigFormBuilder
{
    public static String PROP_ID = "id";
    public static String PROP_MODULECLASS = "moduleClass";
    
    List<Field<?>> labels = new ArrayList<Field<?>>();
    List<Field<?>> textBoxes = new ArrayList<Field<?>>();
    List<Field<?>> checkBoxes = new ArrayList<Field<?>>();
    List<Component> otherWidgets = new ArrayList<Component>();
    
    
    public String getTitle(ModuleConfig config)
    {
        return getPrettyName(config.getClass().getSimpleName());
    }
    
    
    @SuppressWarnings("serial")
    @Override
    public FormLayout buildForm(final FieldGroup fieldGroup)
    {
        reset();
        FormLayout form = new FormLayout();
        
        // add widget for each visible attribute
        for (Object propId: fieldGroup.getUnboundPropertyIds())
        {
            Property<?> prop = fieldGroup.getItemDataSource().getItemProperty(propId);
            
            if (prop instanceof ContainerProperty)
            {
                String label = ((ContainerProperty)prop).getLabel();
                if (label == null)
                    label = getPrettyName((String)propId);
                
                /*Table table = new Table();
                table.setCaption(label);
                table.setSizeFull();
                table.setPageLength(5);
                table.setHeight(50, Unit.POINTS);
                table.setSelectable(true);
                table.setEditable(true);
                table.setColumnReorderingAllowed(false);
                table.setContainerDataSource(((ContainerProperty)prop).getValue());
                table.setBuffered(true);
                table.setStyleName(Runo.TABLE_SMALL);
                otherWidgets.add(table);*/
                
                if (!((ContainerProperty)prop).getValue().getItemIds().isEmpty())
                {
                    Object firstItemId = ((ContainerProperty)prop).getValue().getItemIds().iterator().next();
                    FieldGroup newFieldGroup = new FieldGroup(((ContainerProperty)prop).getValue().getItem(firstItemId));
                    FormLayout subform = new GenericConfigFormBuilder().buildForm(newFieldGroup);
                    subform.setCaption(label);
                    otherWidgets.add(subform);
                }
            }
            else
            {
                Field<?> field = null;
                
                try
                {
                    String label = null;
                    if (prop instanceof FieldProperty)
                        label = ((FieldProperty)prop).getLabel();
                    if (label == null)
                        label = getPrettyName((String)propId);
                    field = fieldGroup.buildAndBind(label, propId);
                }
                catch (Exception e)
                {
                    System.err.println("No UI generator for field " + propId);
                    continue;
                }
                
                //Property<?> prop = field.getPropertyDataSource();            
                customizeField((String)propId, prop, field);
                
                if (field instanceof Label)
                    labels.add(field);
                else if (field instanceof TextField)
                    textBoxes.add(field);
                else if (field instanceof CheckBox)
                    checkBoxes.add(field);
                else
                    otherWidgets.add(field);
            }
        }
        
        // add all widgets
        for (Field<?> w: labels)
            form.addComponent(w);
        for (Field<?> w: textBoxes)
            form.addComponent(w);
        for (Field<?> w: checkBoxes)
            form.addComponent(w);
        for (Component w: otherWidgets)
            form.addComponent(w);        
        
        // add save button
        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setWidth(100.0f, Unit.PERCENTAGE);
        //buttonsLayout.setSizeFull();
        buttonsLayout.setMargin(true);
        buttonsLayout.setSpacing(true);
        form.addComponent(buttonsLayout);
        
        Button saveButton = new Button("Save");
        saveButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event)
            {
                try
                {
                    fieldGroup.commit();
                }
                catch (CommitException e)
                {
                    e.printStackTrace();
                }                
            }
        });
        buttonsLayout.addComponent(saveButton);
        //buttonsLayout.setComponentAlignment(saveButton, Alignment.MIDDLE_CENTER);
        
        // add cancel button
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(ClickEvent event)
            {
                fieldGroup.discard();
            }
        });
        buttonsLayout.addComponent(cancelButton);
        //buttonsLayout.setComponentAlignment(cancelButton, Alignment.MIDDLE_CENTER);
        
        return form;
    }
    
    
    /**
     * Allows to customize the field after creation by buildAndBind
     * TODO customizeField method description
     * @param propId
     * @param prop
     * @param field
     */
    protected void customizeField(String propId, Property<?> prop, Field<?> field)
    {
        if (propId.equals(PROP_ID))
            field.setReadOnly(true);
        else if (propId.equals(PROP_MODULECLASS))
            field.setVisible(false);
        
        if (prop.getType().equals(String.class))
            field.setWidth(250, Unit.PIXELS);
        else if (prop.getType().equals(int.class) || prop.getType().equals(Integer.class))
            field.setWidth(50, Unit.PIXELS);
        else if (prop.getType().equals(float.class) || prop.getType().equals(Float.class))
            field.setWidth(50, Unit.PIXELS);
        else if (prop.getType().equals(double.class) || prop.getType().equals(Double.class))
            field.setWidth(50, Unit.PIXELS);
    }
    
    
    protected String getPrettyName(String text)
    {
        StringBuilder buf = new StringBuilder(text.substring(text.lastIndexOf('.')+1));
        for (int i=0; i<buf.length()-1; i++)
        {
            char c = buf.charAt(i);
            
            if (i == 0)
            {
                char newcar = Character.toUpperCase(c);
                buf.setCharAt(i, newcar);
            }
                    
            else if (Character.isUpperCase(c) && Character.isLowerCase(buf.charAt(i+1)))
            {
                buf.insert(i, ' ');
                i++;
            }
        }
        
        return buf.toString();
    }
    
    
    protected void reset()
    {
        labels.clear();
        textBoxes.clear();
        checkBoxes.clear();
        otherWidgets.clear();
    }
}
