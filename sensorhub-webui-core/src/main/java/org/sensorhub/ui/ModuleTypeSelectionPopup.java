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
import java.util.UUID;
import org.sensorhub.api.module.IModuleProvider;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.SensorHub;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;


public class ModuleTypeSelectionPopup extends Window
{
    private static final long serialVersionUID = -5368554789542357015L;
    private static final String PROP_NAME = "name";
    private static final String PROP_VERSION = "version";
    
    
    protected interface ModuleTypeSelectionCallback
    {
        public void configSelected(Class<?> moduleType, ModuleConfig config);
    }
    
    
    public ModuleTypeSelectionPopup(final Class<?> moduleType, final ModuleTypeSelectionCallback callback)
    {
        super("Select Module Type");
        VerticalLayout layout = new VerticalLayout();
        
        // generate table with module list
        final Table table = new Table();
        table.setSizeFull();
        table.setSelectable(true);
        table.setColumnReorderingAllowed(true);        
        table.addContainerProperty(PROP_NAME, String.class, null);
        table.addContainerProperty(PROP_VERSION, String.class, null);
        table.addContainerProperty("desc", String.class, null);
        table.addContainerProperty("author", String.class, null);
        table.setColumnHeaders(new String[] {"Module Type", "Version", "Description", "Author"});
        table.setPageLength(10);
        table.setMultiSelect(false);
        
        final Map<Object, IModuleProvider> providerMap = new HashMap<Object, IModuleProvider>();
        for (IModuleProvider provider: SensorHub.getInstance().getModuleRegistry().getInstalledModuleTypes())
        {
            Class<?> configClass = provider.getModuleConfigClass();
            Class<?> moduleClass = provider.getModuleClass();
            if (moduleType.isAssignableFrom(configClass) || moduleType.isAssignableFrom(moduleClass))
            {
                Object id = table.addItem(new Object[] {
                        provider.getModuleName(),
                        provider.getModuleVersion(),
                        provider.getModuleDescription(),
                        provider.getProviderName()}, null);
                providerMap.put(id, provider);
            }
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
                String name = null;
                
                try
                {
                    if (selectedItemId != null)
                    {
                        name = (String)table.getContainerProperty(selectedItemId, PROP_NAME).getValue();
                                        
                        IModuleProvider provider = providerMap.get(selectedItemId);
                        Class<?> configClass = provider.getModuleConfigClass();
                        ModuleConfig config = (ModuleConfig)configClass.newInstance();
                        config.id = UUID.randomUUID().toString();
                        config.moduleClass = provider.getModuleClass().getCanonicalName();
                        config.name = "New " + name;
                        config.enabled = false;
                        
                        // send back new config object
                        callback.configSelected(moduleType, config); 
                    }
                }
                catch (Exception e)
                {
                    close();
                    String version = (String)table.getContainerProperty(selectedItemId, PROP_VERSION).getValue();
                    Notification.show("Cannot instantiate module " + name + " v" + version, null, Notification.Type.ERROR_MESSAGE);
                }
                finally
                {
                    close();
                }
            }
        });
        layout.addComponent(okButton);
        layout.setComponentAlignment(okButton, Alignment.MIDDLE_CENTER);
        
        setContent(layout);
        center();
    }
}
