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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.api.UIConstants;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;


/**
 * <p>
 * Admin panel for sensor modules.<br/>
 * This adds a section to view structure of inputs and outputs,
 * and allows the user to send commands and view output data values.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since 1.0
 */
public class SensorAdminPanel extends DefaultModulePanel<ISensorModule<?>> implements IModuleAdminPanel<ISensorModule<?>>
{
    private static final long serialVersionUID = 9206002459600214988L;
    private static final Resource REFRESH_ICON = new ThemeResource("icons/refresh.gif");
    Panel obsPanel, statusPanel, commandsPanel;
    
    
    @Override
    public void build(final MyBeanItem<ModuleConfig> beanItem, final ISensorModule<?> module)
    {
        super.build(beanItem, module);       
        
        // add section label
        final VerticalLayout form = new VerticalLayout();
        form.setWidth(100.0f, Unit.PERCENTAGE);
        form.setMargin(false);
        form.setSpacing(true);
        
        // section title
        form.addComponent(new Label(""));
        HorizontalLayout titleBar = new HorizontalLayout();
        titleBar.setSpacing(true);
        Label sectionLabel = new Label("Inputs/Outputs");
        sectionLabel.addStyleName(UIConstants.STYLE_H3);
        sectionLabel.addStyleName(UIConstants.STYLE_COLORED);
        titleBar.addComponent(sectionLabel);
        titleBar.setComponentAlignment(sectionLabel, Alignment.MIDDLE_LEFT);
        
        // refresh button to show latest record
        Button refreshButton = new Button();
        refreshButton.setDescription("Refresh Data");
        refreshButton.setIcon(REFRESH_ICON);
        refreshButton.addStyleName(UIConstants.STYLE_QUIET);
        titleBar.addComponent(refreshButton);
        titleBar.setComponentAlignment(refreshButton, Alignment.MIDDLE_LEFT);
        refreshButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;
            @Override
            public void buttonClick(ClickEvent event)
            {
                rebuildSwePanels(form, module);
            }
        });
                
        form.addComponent(titleBar);
        
        // add I/O panel only if module is loaded
        rebuildSwePanels(form, module);
        addComponent(form);
    }
    
    
    protected void rebuildSwePanels(VerticalLayout form, ISensorModule<?> module)
    {
        if (module != null)
        {
            SWECommonForm sweFormBuilder = new SWECommonForm();
            Panel oldPanel;
            
            // measurement outputs
            oldPanel = obsPanel;
            obsPanel = newPanel("Observation Outputs");
            for (ISensorDataInterface output: module.getObservationOutputs().values())
            {
                DataComponent dataStruct = output.getRecordDescription().copy();
                DataBlock latestRecord = output.getLatestRecord();
                if (latestRecord != null)
                    dataStruct.setData(latestRecord);
                
                // data structure
                Component sweForm = sweFormBuilder.buildForm(dataStruct);
                ((Layout)obsPanel.getContent()).addComponent(sweForm);
            }  
            
            if (oldPanel != null)
                form.replaceComponent(oldPanel, obsPanel);
            else
                form.addComponent(obsPanel);
            
            // status outputs
            oldPanel = statusPanel;
            statusPanel = newPanel("Status Outputs");
            for (ISensorDataInterface output: module.getStatusOutputs().values())
            {
                Component sweForm = sweFormBuilder.buildForm(output.getRecordDescription());
                ((Layout)statusPanel.getContent()).addComponent(sweForm);
            }           

            if (oldPanel != null)
                form.replaceComponent(oldPanel, statusPanel);
            else
                form.addComponent(statusPanel);
            
            // command inputs
            oldPanel = commandsPanel;
            commandsPanel = newPanel("Command Inputs");
            for (ISensorControlInterface input: module.getCommandInputs().values())
            {
                Component sweForm = sweFormBuilder.buildForm(input.getCommandDescription());
                ((Layout)commandsPanel.getContent()).addComponent(sweForm);
            }           

            if (oldPanel != null)
                form.replaceComponent(oldPanel, commandsPanel);
            else
                form.addComponent(commandsPanel);
        }
    }
    
    
    protected Panel newPanel(String title)
    {
        Panel panel = new Panel(title);
        GridLayout layout = new GridLayout(2, 10);
        layout.setMargin(true);
        panel.setContent(layout);
        return panel;
    }
}
