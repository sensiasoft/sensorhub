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
import org.sensorhub.ui.ValueEntryPopup.ValueCallback;
import org.sensorhub.ui.api.IModuleConfigForm;
import org.sensorhub.ui.api.UIConstants;
import org.sensorhub.ui.data.ComplexProperty;
import org.sensorhub.ui.data.ContainerProperty;
import org.sensorhub.ui.data.FieldProperty;
import org.sensorhub.ui.data.MyBeanItem;
import org.sensorhub.ui.data.MyBeanItemContainer;
import com.vaadin.data.Property;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitEvent;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.fieldgroup.FieldGroup.CommitHandler;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.CloseHandler;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;
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
        if (title == null || title.length() == 0)
            title = DisplayUtils.getPrettyName(propId);
        
        build(title, prop.getValue());
    }
    
    
    @Override
    public void build(String title, MyBeanItem<? extends Object> beanItem)
    {
        List<Field<?>> labels = new ArrayList<Field<?>>();
        List<Field<?>> textBoxes = new ArrayList<Field<?>>();
        List<Field<?>> listBoxes = new ArrayList<Field<?>>();
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
                    Class<?> eltType = ((ContainerProperty)prop).getValue().getBeanType();
                    
                    // use simple table for string lists
                    if (eltType == String.class)
                    {
                        Component list = buildSimpleTable((String)propId, (ContainerProperty)prop);
                        fieldGroup.bind((Field<?>)list, propId);
                        listBoxes.add((Field<?>)list);
                    }
                    
                    // else use tab sheet
                    else if (!((ContainerProperty)prop).getValue().getItemIds().isEmpty())
                    {
                        Component subform = buildTabs((String)propId, (ContainerProperty)prop, fieldGroup);
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
                    Class<?> propType = prop.getType();
                    if (propType.equals(String.class))
                    {
                        if (field instanceof Label)
                            labels.add(field);
                        else
                            textBoxes.add(field);
                    }
                    else if (Enum.class.isAssignableFrom(propType))
                        numberBoxes.add(field);
                    else if (Number.class.isAssignableFrom(propType))
                        numberBoxes.add(field);
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
        for (Field<?> w: listBoxes)
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
        Class<?> propType = prop.getType();
        
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
        
        if (propType.equals(String.class))
            field.setWidth(500, Unit.PIXELS);
        else if (propType.equals(int.class) || propType.equals(Integer.class) ||
                propType.equals(float.class) || propType.equals(Float.class) ||
                propType.equals(double.class) || propType.equals(Double.class))
            field.setWidth(200, Unit.PIXELS);
        else if (Enum.class.isAssignableFrom(propType))
            ((ListSelect)field).setRows(3);
        
        if (field instanceof TextField) {
            ((TextField)field).setNullSettingAllowed(true);
            ((TextField)field).setNullRepresentation("");
        }
        
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
    
    
    protected Component buildSimpleTable(final String propId, final ContainerProperty prop)
    {
        String label = prop.getLabel();
        if (label == null)
            label = DisplayUtils.getPrettyName((String)propId);
        
        final MyBeanItemContainer<Object> container = prop.getValue();
        final ListSelect listBox = new ListSelect(label, container);
        listBox.setValue(container);
        listBox.setItemCaptionMode(ItemCaptionMode.PROPERTY);
        listBox.setItemCaptionPropertyId(MyBeanItem.PROP_VALUE);
        listBox.setImmediate(true);
        listBox.setBuffered(true);
        listBox.setNullSelectionAllowed(false);
        listBox.setDescription(prop.getDescription());
        listBox.setWidth(500, Unit.PIXELS);
        listBox.setRows(Math.max(2, Math.min(5, container.size())));
        
        FieldWrapper<Object> field = new FieldWrapper<Object>(listBox) {
            private static final long serialVersionUID = 1499878131611223989L;
            protected Component initContent()
            {
                HorizontalLayout layout = new HorizontalLayout();
                layout.setSpacing(true);
                
                // inner field
                layout.addComponent(innerField);
                layout.setComponentAlignment(innerField, Alignment.MIDDLE_LEFT);
                
                VerticalLayout buttons = new VerticalLayout();
                layout.addComponent(buttons);
                
                // add button
                Button addBtn = new Button(ADD_ICON);
                addBtn.addStyleName(STYLE_QUIET);
                addBtn.addStyleName(STYLE_SMALL);
                buttons.addComponent(addBtn);
                addBtn.addClickListener(new ClickListener() {
                    private static final long serialVersionUID = 1L;
                    public void buttonClick(ClickEvent event)
                    {
                        ValueEntryPopup popup = new ValueEntryPopup(500, new ValueCallback() {
                            @Override
                            public void newValue(String value)
                            {
                                container.addBean(value);
                                // grow list size with max at 5
                                listBox.setRows(Math.max(2, Math.min(5, container.size())));
                            }
                        });
                        popup.setModal(true);
                        AdminUI.getInstance().addWindow(popup);
                    }
                });
                
                // remove button
                Button delBtn = new Button(DEL_ICON);
                delBtn.addStyleName(STYLE_QUIET);
                delBtn.addStyleName(STYLE_SMALL);
                buttons.addComponent(delBtn);
                delBtn.addClickListener(new ClickListener() {
                    private static final long serialVersionUID = 1L;
                    public void buttonClick(ClickEvent event)
                    {
                        Object itemId = listBox.getValue();
                        container.removeItem(itemId);
                    }
                });
                                
                return layout;
            }
            
            @Override
            public void commit() throws SourceException, InvalidValueException
            {
                // override commit here because the ListSelect setValue() method
                // only sets the index of the selected item, and not the list content
                prop.setValue(container);
            }             
        };
        
        return field;
    }
    
    
    protected Component buildTabs(final String propId, final ContainerProperty prop, final FieldGroup fieldGroup)
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
        sectionLabel.setDescription(prop.getDescription());
        sectionLabel.addStyleName(STYLE_H3);
        sectionLabel.addStyleName(STYLE_COLORED);
        titleBar.addComponent(sectionLabel);
        layout.addComponent(titleBar);
        
        // create one tab per item in container
        final MyBeanItemContainer<Object> container = prop.getValue();
        final TabSheet tabs = new TabSheet();
        tabs.setSizeFull();
        int i = 1;
        for (Object itemId: container.getItemIds())
        {
            MyBeanItem<Object> childBeanItem = (MyBeanItem<Object>)container.getItem(itemId);
            IModuleConfigForm subform = AdminUI.getInstance().generateForm(childBeanItem.getBean().getClass());
            subform.build(null, childBeanItem);
            ((MarginHandler)subform).setMargin(new MarginInfo(true, false, true, false));
            allForms.add(subform);
            Tab tab = tabs.addTab(subform, "Item #" + (i++));
            tab.setClosable(true);
            
            // store item id so we can map a tab with the corresponding bean item
            ((AbstractComponent)subform).setData(itemId);
        }
        
        // draw icon on last tab to add new items
        tabs.addTab(new VerticalLayout(), "", UIConstants.ADD_ICON);
        
        // catch close event to delete item
        tabs.setCloseHandler(new CloseHandler() {
            private static final long serialVersionUID = 1L;
            @Override
            public void onTabClose(TabSheet tabsheet, Component tabContent)
            {
                final Tab tab = tabs.getTab(tabContent);
                
                final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to delete " + tab.getCaption() + "?</br>All settings will be lost.");
                popup.addCloseListener(new CloseListener() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void windowClose(CloseEvent e)
                    {
                        if (popup.isConfirmed())
                        {                    
                            // retrieve id of item shown on tab
                            AbstractComponent tabContent = (AbstractComponent)tab.getComponent();
                            Object itemId = tabContent.getData();
                            
                            // remove from UI
                            int deletedTabPos = tabs.getTabPosition(tab);
                            tabs.removeTab(tab);
                            tabs.setSelectedTab(deletedTabPos-1);
                            
                            // remove from container
                            container.removeItem(itemId);
                        }
                    }                        
                });
                
                popup.setModal(true);
                AdminUI.getInstance().addWindow(popup);
            }
        });
        
        // catch select event on '+' tab to add new item
        tabs.addSelectedTabChangeListener(new SelectedTabChangeListener() {
            private static final long serialVersionUID = 1L;
            public void selectedTabChange(SelectedTabChangeEvent event)
            {
                Component selectedTab = event.getTabSheet().getSelectedTab();
                final Tab tab = tabs.getTab(selectedTab);
                final int selectedTabPos = tabs.getTabPosition(tab);
                                
                // case of + tab to add new item
                if (tab.getCaption().equals(""))
                {
                    tabs.setSelectedTab(selectedTabPos-1);
                    
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
                                    // add new item to container
                                    MyBeanItem<Object> childBeanItem = container.addBean(objectType.newInstance(), ((String)propId) + PROP_SEP);
                                                                        
                                    // generate form for new item
                                    IModuleConfigForm subform = AdminUI.getInstance().generateForm(childBeanItem.getBean().getClass());
                                    subform.build(null, childBeanItem);
                                    ((MarginHandler)subform).setMargin(new MarginInfo(true, false, true, false));
                                    allForms.add(subform);
                                    
                                    // add new tab and select it
                                    Tab newTab = tabs.addTab(subform, "Item #" + (selectedTabPos+1), null, selectedTabPos);
                                    newTab.setClosable(true);
                                    tabs.setSelectedTab(newTab);
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
        
        // also register commit handler
        fieldGroup.addCommitHandler(new CommitHandler() {
            private static final long serialVersionUID = 1L;
            @Override
            public void preCommit(CommitEvent commitEvent) throws CommitException
            {                               
            }

            @Override
            public void postCommit(CommitEvent commitEvent) throws CommitException
            {
                // make sure new items are transfered to model
                prop.setValue(prop.getValue());
            }
        });
        
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
