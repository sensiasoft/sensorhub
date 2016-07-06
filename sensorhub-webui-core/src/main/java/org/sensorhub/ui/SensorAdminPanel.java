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

import java.util.Timer;
import java.util.TimerTask;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
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
import com.vaadin.ui.UI;
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
    Panel obsPanel, statusPanel, commandsPanel;
    
    
    @Override
    public void build(final MyBeanItem<ModuleConfig> beanItem, final ISensorModule<?> module)
    {
        super.build(beanItem, module);       
        
        // add section label
        final GridLayout form = new GridLayout();
        form.setWidth(100.0f, Unit.PERCENTAGE);
        form.setMargin(false);
        form.setSpacing(true);
        
        // sensor info panel
        if (module.isInitialized())
        {
            Label sectionLabel = new Label("Sensor Info");
            sectionLabel.addStyleName(STYLE_H3);
            sectionLabel.addStyleName(STYLE_COLORED);
            form.addComponent(sectionLabel);
            form.addComponent(new Label("<b>Unique ID:</b> " + module.getUniqueIdentifier(), ContentMode.HTML));
            AbstractFeature foi = module.getCurrentFeatureOfInterest();
            if (foi != null)
                form.addComponent(new Label("<b>FOI ID:</b> " + foi.getUniqueIdentifier(), ContentMode.HTML));
        }
        
        // section title
        form.addComponent(new Label("&nbsp;", ContentMode.HTML));
        HorizontalLayout titleBar = new HorizontalLayout();
        titleBar.setSpacing(true);
        Label sectionLabel = new Label("Inputs/Outputs");
        sectionLabel.addStyleName(STYLE_H3);
        sectionLabel.addStyleName(STYLE_COLORED);
        titleBar.addComponent(sectionLabel);
        titleBar.setComponentAlignment(sectionLabel, Alignment.MIDDLE_LEFT);
        
        // refresh button to show latest record
        final Timer timer = new Timer();
        final Button refreshButton = new Button("Refresh");
        refreshButton.setDescription("Toggle auto-refresh data once per second");
        refreshButton.setIcon(REFRESH_ICON);
        refreshButton.addStyleName(STYLE_SMALL);
        refreshButton.addStyleName(STYLE_QUIET);
        refreshButton.setData(false);
        titleBar.addComponent(refreshButton);
        titleBar.setComponentAlignment(refreshButton, Alignment.MIDDLE_LEFT);
        refreshButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;
            TimerTask autoRefreshTask;
            public void buttonClick(ClickEvent event)
            {
                // toggle button state
                boolean state = !(boolean)refreshButton.getData();
                refreshButton.setData(state);
                
                if (state)
                {
                    autoRefreshTask = new TimerTask()
                    {
                        public void run()
                        {
                            final UI ui = SensorAdminPanel.this.getUI();
                            
                            if (ui != null)
                            {
                                ui.access(new Runnable() {
                                    public void run()
                                    {
                                        rebuildSwePanels(form, module);
                                        ui.push();
                                    }
                                });
                            }
                            else
                                cancel(); // if panel was detached
                        }
                    };
                    timer.schedule(autoRefreshTask, 0L, 1000L);                    
                    refreshButton.setIcon(FontAwesome.TIMES);
                    refreshButton.setCaption("Stop");
                }
                else
                {
                    autoRefreshTask.cancel();
                    refreshButton.setIcon(REFRESH_ICON);
                    refreshButton.setCaption("Refresh");
                }
            }
        });
                
        form.addComponent(titleBar);
        
        // add I/O panel
        rebuildSwePanels(form, module);
        addComponent(form);
    }
    
    
    protected void rebuildSwePanels(GridLayout form, ISensorModule<?> module)
    {
        if (module != null)
        {
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
                Component sweForm = new SWECommonForm(dataStruct);
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
                Component sweForm = new SWECommonForm(output.getRecordDescription());
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
                Component sweForm = new SWECommonForm(input.getCommandDescription());
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
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setDefaultComponentAlignment(Alignment.TOP_LEFT);
        panel.setContent(layout);
        return panel;
    }
}
