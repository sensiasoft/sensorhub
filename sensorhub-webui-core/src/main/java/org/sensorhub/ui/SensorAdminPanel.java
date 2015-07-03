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

import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
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
    
    
    @Override
    public void build(MyBeanItem<ModuleConfig> beanItem, ISensorModule<?> module)
    {
        super.build(beanItem, module);       
        
        // add section label
        VerticalLayout form = new VerticalLayout();
        form.setWidth(100.0f, Unit.PERCENTAGE);
        form.setMargin(false);
        form.setSpacing(true);
        Label sectionLabel = new Label("Inputs/Outputs");
        sectionLabel.addStyleName(AdminUI.STYLE_H3);
        sectionLabel.addStyleName(AdminUI.STYLE_COLORED);
        form.addComponent(sectionLabel);
        
        // add I/O panel only if module is loaded and started
        if (module != null)
        {
            Panel ioPanel;
            SWECommonFormBuilder sweFormBuilder = new SWECommonFormBuilder();
            
            // measurement outputs
            ioPanel = newPanel("Observation Outputs");
            for (ISensorDataInterface output: module.getObservationOutputs().values())
            {
                Component sweForm = sweFormBuilder.buildForm(output.getRecordDescription());
                ((Layout)ioPanel.getContent()).addComponent(sweForm);
            }
            form.addComponent(ioPanel);
            
            // status outputs
            ioPanel = newPanel("Status Outputs");
            for (ISensorDataInterface output: module.getStatusOutputs().values())
            {
                Component sweForm = sweFormBuilder.buildForm(output.getRecordDescription());
                ((Layout)ioPanel.getContent()).addComponent(sweForm);
            }           
            form.addComponent(ioPanel);
        }
        else
        {
            
        }
                
        addComponent(form);
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
