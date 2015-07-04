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
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.ui.ModuleTypeSelectionPopup.ModuleTypeSelectionCallback;
import org.sensorhub.ui.api.IModuleConfigForm;
import org.sensorhub.ui.data.ComplexProperty;
import org.sensorhub.ui.data.ContainerProperty;
import org.sensorhub.ui.data.FieldProperty;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
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
public class GenericConfigForm extends VerticalLayout implements IModuleConfigForm
{
    private static final long serialVersionUID = 3491784756273165916L;
    
    protected static final String PROP_ID = "id";
    protected static final String PROP_NAME = "name";
    protected static final String PROP_ENABLED = "enabled";
    protected static final String PROP_MODULECLASS = "moduleClass";
    
    protected FieldGroup fieldGroup;
    
    
    public boolean canUpdateInstance()
    {
        return false;
    }
    
    
    @Override
    public void build(String title, MyBeanItem<? extends Object> beanItem)
    {
        List<Field<?>> labels = new ArrayList<Field<?>>();
        List<Field<?>> textBoxes = new ArrayList<Field<?>>();
        List<Field<?>> checkBoxes = new ArrayList<Field<?>>();
        List<Component> otherWidgets = new ArrayList<Component>();
        
        // prepare header and form layout
        setSpacing(true);
        
        // add main form widgets
        FormLayout form = new FormLayout();
        form.setWidth(100.0f, Unit.PERCENTAGE);
        form.setMargin(false);
        Label sectionLabel = new Label(title);
        sectionLabel.addStyleName(AdminUI.STYLE_H3);
        sectionLabel.addStyleName(AdminUI.STYLE_COLORED);
        form.addComponent(sectionLabel);
        
        // add widget for each visible attribute
        fieldGroup = new FieldGroup(beanItem);
        for (Object propId: fieldGroup.getUnboundPropertyIds())
        {
            Property<?> prop = fieldGroup.getItemDataSource().getItemProperty(propId);
            
            if (prop instanceof ContainerProperty)
            {
                String label = ((ContainerProperty)prop).getLabel();
                if (label == null)
                    label = DisplayUtils.getPrettyName((String)propId);
                
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
                    /*Object firstItemId = ((ContainerProperty)prop).getValue().getItemIds().iterator().next();
                    MyBeanItem<Object> childBeanItem = (MyBeanItem<Object>)((ContainerProperty)prop).getValue().getItem(firstItemId);
                    Component subform = new GenericConfigForm().buildForm(label, childBeanItem);
                    otherWidgets.add(subform);*/
                }
            }
            else if (prop instanceof ComplexProperty)
            {
                Component subform = buildSubForm(propId, (ComplexProperty)prop);
                otherWidgets.add(subform);
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
                        label = DisplayUtils.getPrettyName((String)propId);
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
        
        // main form
        for (Field<?> w: labels)
            form.addComponent(w);
        for (Field<?> w: textBoxes)
            form.addComponent(w);
        for (Field<?> w: checkBoxes)
            form.addComponent(w);
        addComponent(form);
        
        // sub forms
        for (Component w: otherWidgets)
            addComponent(w); 
    }
    
    
    /**
     * Allows to customize the field after creation by buildAndBind
     * @param propId
     * @param prop
     * @param field
     */
    protected void customizeField(String propId, Property<?> prop, Field<?> field)
    {
        if (propId.equals(PROP_ID))
            field.setReadOnly(true);
        else if (propId.endsWith("." + PROP_ID))
            field.setVisible(false);
        else if (propId.endsWith("." + PROP_NAME))
            field.setVisible(false);
        else if (propId.endsWith(PROP_MODULECLASS))
            field.setReadOnly(true);        
        
        if (prop.getType().equals(String.class))
            field.setWidth(500, Unit.PIXELS);
        else if (prop.getType().equals(int.class) || prop.getType().equals(Integer.class))
            field.setWidth(100, Unit.PIXELS);
        else if (prop.getType().equals(float.class) || prop.getType().equals(Float.class))
            field.setWidth(100, Unit.PIXELS);
        else if (prop.getType().equals(double.class) || prop.getType().equals(Double.class))
            field.setWidth(100, Unit.PIXELS);
    } 
    
    
    protected Component buildSubForm(final Object propId, final ComplexProperty prop)
    {
        String label = ((ComplexProperty)prop).getLabel();
        if (label == null)
            label = DisplayUtils.getPrettyName((String)propId);
        final String title = label;
        
        MyBeanItem<Object> childBeanItem = ((ComplexProperty)prop).getValue();
        IModuleConfigForm subform = AdminUI.getInstance().generateForm(childBeanItem.getBean().getClass());
        subform.build(title, childBeanItem);
        
        if (subform.canUpdateInstance())
        {
            final Button chgButton = new Button("Change");
            chgButton.addStyleName(AdminUI.STYLE_QUIET);
            //chgButton.addStyleName(AdminUI.STYLE_SECTION_BUTTONS);
            //chgButton.setIcon(APPLY_ICON);       
            
            chgButton.addClickListener(new ClickListener() {
                private static final long serialVersionUID = 1L;
                @Override
                public void buttonClick(ClickEvent event)
                {
                    // show popup to select among available module types
                    ModuleTypeSelectionPopup popup = new ModuleTypeSelectionPopup(ICommProvider.class, new ModuleTypeSelectionCallback() {
                        public void newConfig(Class<?> moduleType, ModuleConfig config)
                        {
                            config.id = null;
                            config.name = null;
                            MyBeanItem<Object> newItem = new MyBeanItem<Object>(config, propId + ".");
                            prop.setValue(newItem);
                            IModuleConfigForm newForm = AdminUI.getInstance().generateForm(config.getClass());
                            newForm.build(title, newItem);
                            newForm.addComponent(chgButton);
                            replaceComponent((Component)chgButton.getData(), newForm);
                            chgButton.setData(newForm);
                        }
                    });
                    popup.setModal(true);
                    AdminUI.getInstance().addWindow(popup);
                }
            });
            chgButton.setData(subform);
            subform.addComponent(chgButton);
        }
        
        return subform;
    }


    @Override
    public void commit() throws CommitException
    {
        fieldGroup.commit();
        
        for (Component c: this)
        {
            if (c instanceof IModuleConfigForm)
                ((IModuleConfigForm) c).commit();
        }
    }
}
