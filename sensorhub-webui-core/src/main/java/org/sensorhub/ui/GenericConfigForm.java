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
import com.vaadin.ui.GridLayout;
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
    
    
    @Override
    public void build(String propId, ComplexProperty prop)
    {
        String title = prop.getLabel();
        if (title == null)
            title = DisplayUtils.getPrettyName(propId);
        
        build(title, prop.getValue());
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
        if (beanItem != null)
        {
            fieldGroup = new FieldGroup(beanItem);
            for (Object propId: fieldGroup.getUnboundPropertyIds())
            {
                Property<?> prop = fieldGroup.getItemDataSource().getItemProperty(propId);
                
                // sub objects with multiplicity > 1
                if (prop instanceof ContainerProperty)
                {
                    if (!((ContainerProperty)prop).getValue().getItemIds().isEmpty())
                    {
                        Component subform = buildTabs((String)propId, (ContainerProperty)prop);
                        otherWidgets.add(subform);
                    }
                }
                
                // sub object
                else if (prop instanceof ComplexProperty)
                {
                    Component subform = buildSubForm((String)propId, (ComplexProperty)prop);
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
        else if (propId.endsWith(PROP_ENABLED))
            field.setVisible(false);
        else if (propId.endsWith(PROP_MODULECLASS))
            field.setReadOnly(true);        
        
        if (prop.getType().equals(String.class))
            field.setWidth(500, Unit.PIXELS);
        else if (prop.getType().equals(int.class) || prop.getType().equals(Integer.class) ||
                prop.getType().equals(float.class) || prop.getType().equals(Float.class) ||
                prop.getType().equals(double.class) || prop.getType().equals(Double.class))
            field.setWidth(200, Unit.PIXELS);
                
        if (field instanceof TextField)
            ((TextField)field).setNullRepresentation("");
        
        return field;
    } 
    
    
    protected ComponentContainer buildSubForm(final String propId, final ComplexProperty prop)
    {
        Class<?> beanType = prop.getBeanType();
        MyBeanItem<Object> childBeanItem = prop.getValue();
        
        // generate custom form for this bean type
        IModuleConfigForm subform;
        if (childBeanItem != null)
            subform = AdminUI.getInstance().generateForm(childBeanItem.getBean().getClass());
        else
            subform = AdminUI.getInstance().generateForm(beanType);
        subform.build(propId, prop);
        
        // add change button if property is changeable module config
        Class<?> changeableBeanType = subform.getPolymorphicBeanParentType();
        if (changeableBeanType != null)
            addChangeModuleButton(subform, propId, prop, changeableBeanType);
        else if (ModuleConfig.class.isAssignableFrom(beanType))
            addChangeModuleButton(subform, propId, prop, beanType);
        
        // add change button if property can have multiple types
        Map<String, Class<?>> possibleTypes = getPossibleTypes(propId);
        if (!(possibleTypes == null || possibleTypes.isEmpty()))
            addChangeObjectButton(subform, propId, prop, possibleTypes);
        
        if (childBeanItem != null)
            allForms.add(subform);
        
        return subform;
    }
    
    
    protected void addChangeModuleButton(final ComponentContainer parentForm, final String propId, final ComplexProperty prop, final Class<?> objectType)
    {
        final Button chgButton = new Button("Modify");
        //chgButton.addStyleName(STYLE_QUIET);
        chgButton.addStyleName(STYLE_SMALL);
        chgButton.addStyleName(STYLE_SECTION_BUTTONS);
        chgButton.setIcon(REFRESH_ICON);
        
        chgButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;
            public void buttonClick(ClickEvent event)
            {
                // show popup to select among available module types
                ModuleTypeSelectionPopup popup = new ModuleTypeSelectionPopup(objectType, new ModuleTypeSelectionCallback() {
                    public void configSelected(Class<?> moduleType, ModuleConfig config)
                    {
                        // regenerate form
                        config.id = null;
                        config.name = null;
                        MyBeanItem<Object> newItem = new MyBeanItem<Object>(config, propId + ".");
                        prop.setValue(newItem);
                        IModuleConfigForm newForm = AdminUI.getInstance().generateForm(config.getClass());
                        newForm.build(propId, prop);
                        ((VerticalLayout)newForm).addComponent(chgButton, 0);
                                                
                        // replace old form in UI
                        allForms.add(newForm);
                        allForms.remove((IModuleConfigForm)chgButton.getData());
                        replaceComponent((Component)chgButton.getData(), newForm);
                        chgButton.setData(newForm);
                    }
                });
                popup.setModal(true);
                AdminUI.getInstance().addWindow(popup);
            }
        });
        
        chgButton.setData(parentForm);
        ((VerticalLayout)parentForm).addComponent(chgButton, 0);
    }
    
    
    protected void addChangeObjectButton(final ComponentContainer parentForm, final String propId, final ComplexProperty prop, final Map<String, Class<?>> typeList)
    {
        final Button chgButton = new Button("Modify");
        //chgButton.addStyleName(STYLE_QUIET);
        chgButton.addStyleName(STYLE_SMALL);
        chgButton.addStyleName(STYLE_SECTION_BUTTONS);
        chgButton.setIcon(REFRESH_ICON);
        
        chgButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;
            public void buttonClick(ClickEvent event)
            {
                // show popup to select among available module types
                ObjectTypeSelectionPopup popup = new ObjectTypeSelectionPopup("Select Type", typeList, new ObjectTypeSelectionCallback() {
                    public void typeSelected(Class<?> objectType)
                    {
                        try
                        {
                            // regenerate form
                            MyBeanItem<Object> newItem = new MyBeanItem<Object>(objectType.newInstance(), propId + ".");
                            prop.setValue(newItem);
                            IModuleConfigForm newForm = AdminUI.getInstance().generateForm(objectType);
                            newForm.build(propId, prop);
                            ((VerticalLayout)newForm).addComponent(chgButton, 0);
                                                    
                            // replace old form in UI
                            allForms.add(newForm);
                            allForms.remove((IModuleConfigForm)chgButton.getData());
                            replaceComponent((Component)chgButton.getData(), newForm);
                            chgButton.setData(newForm);
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
        });
        chgButton.setData(parentForm);
        ((VerticalLayout)parentForm).addComponent(chgButton, 0);
    }
    
    
    protected Component buildTabs(final String propId, final ContainerProperty prop)
    {
        GridLayout layout = new GridLayout();
        layout.setWidth(100.0f, Unit.PERCENTAGE);
        
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
                            public void typeSelected(Class<?> objectType)
                            {
                                try
                                {
                                    MyBeanItem<Object> childBeanItem = prop.getValue().addBean(objectType.newInstance(), ((String)propId) + PROP_SEP);
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
                final Field<Object> wrapper = this;
                
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
                                wrapper.setValue(module.getLocalID());
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
    public Map<String, Class<?>> getPossibleTypes(String propId)
    {
        return Collections.EMPTY_MAP;
    }


    @Override
    public Class<?> getPolymorphicBeanParentType()
    {
        return null;
    }
}
