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
import org.sensorhub.ui.api.IModuleConfigFormBuilder;
import org.sensorhub.ui.api.IModulePanelBuilder;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.ui.Component;
import com.vaadin.ui.Layout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;


/**
 * <p>
 * Builder for sensor module panels.<br/>
 * This builder adds a section to browse structure of inputs and outputs,
 * and allows the user to send commands and view output data values.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since 1.0
 */
public class SensorPanelBuilder extends DefaultModulePanelBuilder<ISensorModule<?>> implements IModulePanelBuilder<ISensorModule<?>>
{
    SWECommonFormBuilder sweFormBuilder = new SWECommonFormBuilder();
    
    
    @Override
    public Component buildPanel(MyBeanItem<ModuleConfig> beanItem, ISensorModule<?> module, IModuleConfigFormBuilder formBuilder)
    {
        Panel panel = (Panel)super.buildPanel(beanItem, module, formBuilder);       
        
        // add I/O panel only if module is loaded and started
        if (module != null)
        {
            Panel ioPanel;
            
            // measurement outputs
            ioPanel = newPanel("Observation Outputs");
            for (ISensorDataInterface output: module.getObservationOutputs().values())
            {
                Component form = sweFormBuilder.buildForm(output.getRecordDescription());
                ((Layout)ioPanel.getContent()).addComponent(form);
            }
            ((Layout)panel.getContent()).addComponent(ioPanel);
            
            // status outputs
            ioPanel = newPanel("Status Outputs");
            for (ISensorDataInterface output: module.getStatusOutputs().values())
            {
                Component form = sweFormBuilder.buildForm(output.getRecordDescription());
                ((Layout)ioPanel.getContent()).addComponent(form);
            }           
            ((Layout)panel.getContent()).addComponent(ioPanel);
        }
                
        return panel;
    }
    
    
    protected Panel newPanel(String title)
    {
        Panel panel = new Panel(title);
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        panel.setContent(layout);
        return panel;
    }

}
