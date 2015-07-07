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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.ui.ModuleInstanceSelectionPopup.ModuleInstanceSelectionCallback;
import org.sensorhub.ui.ModuleTypeSelectionPopup.ModuleTypeSelectionCallback;
import org.sensorhub.ui.ObjectTypeSelectionPopup.ObjectTypeSelectionCallback;
import org.sensorhub.ui.api.IModuleConfigForm;
import org.sensorhub.ui.api.UIConstants;
import org.sensorhub.ui.data.ComplexProperty;
import org.sensorhub.ui.data.ContainerProperty;
import org.sensorhub.ui.data.FieldProperty;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.data.Property;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.TabSheet.Tab;
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
public class GenericConfigForm extends VerticalLayout implements IModuleConfigForm, UIConstants
{
    private static final long serialVersionUID = 3491784756273165916L;
    
    protected FieldGroup fieldGroup;
    protected List<IModuleConfigForm> allForms = new ArrayList<IModuleConfigForm>();
    
    
    public boolean canUpdateInstance()
    {
        return false;
    }
    
    
    @Override
    public void build(String title, MyBeanItem<? extends Object> beanItem)
    {
        List<Field<?>> labels = new ArrayList<Field<?>>();
        List<Field<?>> textBoxes = new ArrayList<Field<?>>();
        List<Field<?>> numberBoxes = new ArrayList<Field<?>>();
        List<Field<?>> checkBoxes = new ArrayList<Field<?>>();
        List<Component> otherWidgets = new ArrayList<Component>();
        
        // prepare header and form layout
        setSpacing(true);
        
        // add main form widgets
        FormLayout form = new FormLayout();
        form.setWidth(100.0f, Unit.PERCENTAGE);
        form.setMargin(false);
        
        if (title != null)
        {
            Label sectionLabel = new Label(title);
            sectionLabel.addStyleName(STYLE_H3);
            sectionLabel.addStyleName(STYLE_COLORED);
            form.addComponent(sectionLabel);
        }
        
        // add widget for each visible attribute
        fieldGroup = new FieldGroup(beanItem);
        for (Object propId: fieldGroup.getUnboundPropertyIds())
        {
            Property<?> prop = fieldGroup.getItemDataSource().getItemProperty(propId);
            
            // sub objects with multiplicity > 1
            if (prop instanceof ContainerProperty)
            {
                String label = ((ContainerProperty)prop).getLabel();
                if (label == null)
                    label = DisplayUtils.getPrettyName((String)propId);
                
                if (!((ContainerProperty)prop).getValue().getItemIds().isEmpty())
                {
                    Component subform = buildTabs(propId, (ContainerProperty)prop);
                    otherWidgets.add(subform);
                }
            }
            
            // sub object
            else if (prop instanceof ComplexProperty)
            {
                Component subform = buildSubForm(propId, (ComplexProperty)prop);
                otherWidgets.add(subform);
            }
            
            // scalar field
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
                    
                    String desc = null;
                    if (prop instanceof FieldProperty)
                        desc = ((FieldProperty)prop).getDescription();
                    
                    field = buildAndBindField(label, (String)propId, prop);
                    ((AbstractField<?>)field).setDescription(desc);                    
                }
                catch (Exception e)
                {
                    System.err.println("No UI generator for field " + propId);
                    continue;
                }
                
                // add to one of the widget lists so we can order by widget type
                if (field instanceof Label)
                    labels.add(field);
                else if (field instanceof TextField || field instanceof CustomField)
                {
                    if (prop.getType().equals(String.class))
                        textBoxes.add(field);
                    else
                        numberBoxes.add(field);
                }
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
        for (Field<?> w: numberBoxes)
            form.addComponent(w);
        for (Field<?> w: checkBoxes)
            form.addComponent(w);
        addComponent(form);
        
        // sub forms
        for (Component w: otherWidgets)
            addComponent(w); 
    }
    
    
    /**
     * Method called to generate and bind the Field component corresponding to a
     * scalar property
     * @param label
     * @param propId
     * @param prop
     * @return the generated Field object
     */
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop)
    {
        Field<?> field = fieldGroup.buildAndBind(label, propId);

        if (propId.equals(PROP_ID))
            field.setReadOnly(true);
        else if (propId.endsWith("." + PROP_ID))
            field.setVisible(false);
        else if (propId.endsWith("." + PROP_NAME))
            field.setVisible(false);
        else if (propId.endsWith("." + PROP_ENABLED))
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
        
        if (field instanceof TextField)
            ((TextField)field).setNullRepresentation("");
        
        return field;
    } 
    
    
    protected ComponentContainer buildSubForm(final Object propId, final ComplexProperty prop)
    {
        String label = ((ComplexProperty)prop).getLabel();
        if (label == null)
            label = DisplayUtils.getPrettyName((String)propId);
        final String title = label;
        
        MyBeanItem<Object> childBeanItem = ((ComplexProperty)prop).getValue();
        IModuleConfigForm subform = AdminUI.getInstance().generateForm(childBeanItem.getBean().getClass());
        subform.build(title, childBeanItem);
        allForms.add(subform);
        
        if (subform.canUpdateInstance())
        {
            final Button chgButton = new Button("Change");
            chgButton.addStyleName(STYLE_QUIET);
            //chgButton.addStyleName(AdminUI.STYLE_SECTION_BUTTONS);
            //chgButton.setIcon(APPLY_ICON);       
            
            chgButton.addClickListener(new ClickListener() {
                private static final long serialVersionUID = 1L;
                public void buttonClick(ClickEvent event)
                {
                    // show popup to select among available module types
                    ModuleTypeSelectionPopup popup = new ModuleTypeSelectionPopup(ICommProvider.class, new ModuleTypeSelectionCallback() {
                        public void configSelected(Class<?> moduleType, ModuleConfig config)
                        {
                            config.id = null;
                            config.name = null;
                            MyBeanItem<Object> newItem = new MyBeanItem<Object>(config, propId + ".");
                            prop.setValue(newItem);
                            IModuleConfigForm newForm = AdminUI.getInstance().generateForm(config.getClass());
                            newForm.build(title, newItem);
                            newForm.addComponent(chgButton);
                            replaceComponent((Component)chgButton.getData(), newForm);
                            allForms.remove((IModuleConfigForm)chgButton.getData());
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
    
    
    protected Component buildTabs(final Object propId, final ContainerProperty prop)
    {
        VerticalLayout layout = new VerticalLayout();
        
        // title bar
        HorizontalLayout titleBar = new HorizontalLayout();
        titleBar.setMargin(new MarginInfo(true, false, false, false));
        titleBar.setSpacing(true);
        String label = prop.getLabel();
        if (label == null)
            label = DisplayUtils.getPrettyName((String)propId);
        
        Label sectionLabel = new Label(label);
        sectionLabel.addStyleName(STYLE_H3);
        sectionLabel.addStyleName(STYLE_COLORED);
        titleBar.addComponent(sectionLabel);
        layout.addComponent(titleBar);
        
        // create one tab per item in container
        final TabSheet tabs = new TabSheet();
        tabs.setSizeFull();
        int i = 1;
        for (Object itemId: prop.getValue().getItemIds())
        {
            MyBeanItem<Object> childBeanItem = (MyBeanItem<Object>)prop.getValue().getItem(itemId);
            IModuleConfigForm subform = AdminUI.getInstance().generateForm(childBeanItem.getBean().getClass());
            subform.build(null, childBeanItem);
            ((MarginHandler)subform).setMargin(new MarginInfo(true, false, true, false));
            allForms.add(subform);
            tabs.addTab(subform, "Item #" + (i++));
        }
        
        tabs.addSelectedTabChangeListener(new SelectedTabChangeListener() {
            private static final long serialVersionUID = 1L;
            public void selectedTabChange(SelectedTabChangeEvent event)
            {
                Component selectedTab = event.getTabSheet().getSelectedTab();
                final Tab tab = tabs.getTab(selectedTab);
                if (tab.getCaption().equals(""))
                {
                    try
                    {
                        // show popup to select among available module types
                        String title = "Please select the desired option";
                        Map<String, Class<?>> typeList = GenericConfigForm.this.getPossibleTypes(propId);
                        ObjectTypeSelectionPopup popup = new ObjectTypeSelectionPopup(title, typeList, new ObjectTypeSelectionCallback() {
                            public void typeSelected(Class<?> clazz)
                            {
                                try
                                {
                                    MyBeanItem<Object> childBeanItem = prop.getValue().addBean(clazz.newInstance(), ((String)propId) + PROP_SEP);
                                    prop.setValue(prop.getValue());
                                    IModuleConfigForm subform = AdminUI.getInstance().generateForm(childBeanItem.getBean().getClass());
                                    subform.build(null, childBeanItem);
                                    ((MarginHandler)subform).setMargin(new MarginInfo(true, false, true, false));
                                    allForms.add(subform);
                                    int newTabPos = tabs.getTabPosition(tab);
                                    tabs.addTab(subform, "Item #" + (newTabPos+1), null, newTabPos);
                                    tabs.setSelectedTab(newTabPos);
                                }
                                catch (Exception e)
                                {
                                    Notification.show("Error", e.getMessage(), Notification.Type.ERROR_MESSAGE);
                                }
                            }
                        });
                        popup.setModal(true);
                        AdminUI.getInstance().addWindow(popup);                        
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }                
            }
        });
        
        // add icon on last tab
        tabs.addTab(new VerticalLayout(), "", UIConstants.ADD_ICON);
        
        layout.addComponent(tabs);                
        return layout;
    }
    
    
    @SuppressWarnings("rawtypes")
    protected Field<Object> makeModuleSelectField(Field<Object> field, final Class<? extends IModule> moduleType)
    {
        field = new FieldWrapper<Object>(field) {
            private static final long serialVersionUID = -992750405944982226L;
            protected Component initContent()
            {
                HorizontalLayout layout = new HorizontalLayout();
                layout.setSpacing(true);
                
                // inner field
                innerField.setReadOnly(true);
                layout.addComponent(innerField);
                layout.setComponentAlignment(innerField, Alignment.MIDDLE_LEFT);
                
                // select module button
                Button selectBtn = new Button(LINK_ICON);
                selectBtn.addStyleName(STYLE_QUIET);
                layout.addComponent(selectBtn);
                layout.setComponentAlignment(selectBtn, Alignment.MIDDLE_LEFT);
                selectBtn.addClickListener(new ClickListener() {
                    private static final long serialVersionUID = 1L;
                    public void buttonClick(ClickEvent event)
                    {
                        // show popup to select among available module types
                        ModuleInstanceSelectionPopup popup = new ModuleInstanceSelectionPopup(moduleType, new ModuleInstanceSelectionCallback() {
                            public void moduleSelected(IModule module)
                            {
                                innerField.setReadOnly(false);
                                innerField.setValue(module.getLocalID());
                                innerField.setReadOnly(true);
                            }
                        });
                        popup.setModal(true);
                        AdminUI.getInstance().addWindow(popup);
                    }
                });
                                
                return layout;
            }             
        };
        
        return field;
    }


    @Override
    public void commit() throws CommitException
    {
        fieldGroup.commit();        
        for (IModuleConfigForm form: allForms)
            form.commit();
    }


    @Override
    public Map<String, Class<?>> getPossibleTypes(Object propId)
    {
        return Collections.EMPTY_MAP;
    }
}
