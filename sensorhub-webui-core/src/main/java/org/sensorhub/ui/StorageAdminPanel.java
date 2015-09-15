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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.ScalarComponent;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.persistence.DataFilter;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.IRecordStorageModule;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.data.MyBeanItem;
import org.tltv.gantt.Gantt;
import org.tltv.gantt.client.shared.Resolution;
import org.tltv.gantt.client.shared.Step;
import org.tltv.gantt.client.shared.SubStep;
import org.vast.swe.SWEConstants;
import org.vast.swe.ScalarIndexer;
import org.vast.util.DateTimeFormat;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;


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
public class StorageAdminPanel extends DefaultModulePanel<IRecordStorageModule<?>> implements IModuleAdminPanel<IRecordStorageModule<?>>
{
    private static final long serialVersionUID = 9206002459600214988L;
    Panel oldPanel;
    
    
    @Override
    public void build(final MyBeanItem<ModuleConfig> beanItem, final IRecordStorageModule<?> storage)
    {
        super.build(beanItem, storage);       
        
        if (storage != null)
        {            
            // section layout
            final GridLayout form = new GridLayout();
            form.setWidth(100.0f, Unit.PERCENTAGE);
            form.setMargin(false);
            form.setSpacing(true);
            
            // section title
            form.addComponent(new Label(""));
            HorizontalLayout titleBar = new HorizontalLayout();
            titleBar.setSpacing(true);
            Label sectionLabel = new Label("Data Store Content");
            sectionLabel.addStyleName(STYLE_H3);
            sectionLabel.addStyleName(STYLE_COLORED);
            titleBar.addComponent(sectionLabel);
            titleBar.setComponentAlignment(sectionLabel, Alignment.MIDDLE_LEFT);
            
            // refresh button to show latest record
            Button refreshButton = new Button("Refresh");
            refreshButton.setDescription("Reload data from storage");
            refreshButton.setIcon(REFRESH_ICON);
            refreshButton.addStyleName(STYLE_QUIET);
            titleBar.addComponent(refreshButton);
            titleBar.setComponentAlignment(refreshButton, Alignment.MIDDLE_LEFT);
            refreshButton.addClickListener(new ClickListener() {
                private static final long serialVersionUID = 1L;
                @Override
                public void buttonClick(ClickEvent event)
                {
                    buildDataPanel(form, storage);
                }
            });
                    
            form.addComponent(titleBar);
            
            // add I/O panel
            buildDataPanel(form, storage);
            addComponent(form);
        }
    }
    
    
    protected void buildDataPanel(GridLayout form, IRecordStorageModule<?> storage)
    {        
        // measurement outputs
        int i = 1;
        for (IRecordStoreInfo dsInfo: storage.getRecordStores().values())
        {
            Panel panel = newPanel("Stream #" + i++);                
            GridLayout panelLayout = ((GridLayout)panel.getContent());
            panelLayout.setSpacing(true);
            
            // stored time period
            double[] timeRange = storage.getRecordsTimeRange(dsInfo.getName());
            Label l = new Label("<b>Time Range:</b> " + new DateTimeFormat().formatIso(timeRange[0], 0)
                                 + " / " + new DateTimeFormat().formatIso(timeRange[1], 0));
            l.setContentMode(ContentMode.HTML);
            panelLayout.addComponent(l, 0, 0, 1, 0);
            
            // time line
            panelLayout.addComponent(buildGantt(storage, dsInfo), 0, 1, 1, 1);
            
            // data structure
            DataComponent dataStruct = dsInfo.getRecordDescription();
            Component sweForm = new SWECommonForm(dataStruct);
            panelLayout.addComponent(sweForm);
            
            // data table
            panelLayout.addComponent(buildTable(storage, dsInfo));
            
            if (oldPanel != null)
                form.replaceComponent(oldPanel, panel);
            else
                form.addComponent(panel);
            oldPanel = panel;
        }
    }
    
    
    protected Gantt buildGantt(IRecordStorageModule<?> storage, IRecordStoreInfo recordInfo)
    {
        double[] timeRange = storage.getRecordsTimeRange(recordInfo.getName());
        timeRange[0] -= 3600;
        timeRange[1] += 3600;
        
        Gantt gantt = new Gantt();
        gantt.setWidth(100, Unit.PERCENTAGE);
        gantt.setHeight(130, Unit.PIXELS);
        gantt.setResizableSteps(false);
        gantt.setMovableSteps(false);
        gantt.setStartDate(new Date((long)(timeRange[0]*1000)));
        gantt.setEndDate(new Date((long)(timeRange[1]*1000)));        
        gantt.setYearsVisible(false);
        gantt.setTimelineMonthFormat("MMMM yyyy");
        gantt.setResolution(Resolution.Hour);
        
        Step dataTimeRange = new Step(getPrettyName(recordInfo.getRecordDescription()));
        dataTimeRange.setBackgroundColor("FFFFFF");
        dataTimeRange.setStartDate((long)(timeRange[0]*1000));
        dataTimeRange.setEndDate((long)(timeRange[1]*1000));
                
        // add periods when data is actually available
        Iterator<double[]> clusterTimes = storage.getRecordsTimeClusters(recordInfo.getName());
        while (clusterTimes.hasNext())
        {
            timeRange = clusterTimes.next();
            SubStep clusterPeriod = new SubStep();
            clusterPeriod.setStartDate((long)(timeRange[0]*1000));
            clusterPeriod.setEndDate((long)(timeRange[1]*1000));
            dataTimeRange.addSubStep(clusterPeriod);
            
            clusterPeriod.setDescription(
                    new DateTimeFormat().formatIso(timeRange[0], 0) + " / " +
                    new DateTimeFormat().formatIso(timeRange[1], 0)
            );
        }        
        
        gantt.addStep(dataTimeRange);
        
        gantt.addClickListener(new Gantt.ClickListener() {
            private static final long serialVersionUID = 1L;
            public void onGanttClick(org.tltv.gantt.Gantt.ClickEvent event) {
                System.out.println("click");
            }
        });
        
        return gantt;
    }
    
    
    protected Table buildTable(IRecordStorageModule<?> storage, IRecordStoreInfo recordInfo)
    {
        Table table = new Table();
        table.setWidth(100, Unit.PERCENTAGE);
        table.setPageLength(10);
        
        // add column names
        List<ScalarIndexer> indexers = new ArrayList<ScalarIndexer>();
        DataComponent recordDef = recordInfo.getRecordDescription();
        addColumns(recordDef, recordDef, table, indexers);
        
        // add data
        Iterator<DataBlock> it = storage.getDataBlockIterator(new DataFilter(recordInfo.getName()));
        int count = 0;
        int pageSize = 10;
        while (it.hasNext() && count < pageSize)
        {
            DataBlock dataBlk = it.next();
            
            Object[] values = new Object[indexers.size()];
            for (int i=0; i<values.length; i++)
                values[i] = indexers.get(i).getStringValue(dataBlk);
            
            table.addItem(values, count);
            count++;
        }
        
        return table;
    }
    
    
    protected void addColumns(DataComponent recordDef, DataComponent component, Table table, List<ScalarIndexer> indexers)
    {
        if (component instanceof ScalarComponent)
        {
            // add column names
            String propId = component.getName();
            String label = getPrettyName(component);
            table.addContainerProperty(propId, String.class, null, label, null, null);
            
            // correct time formatting
            String def = component.getDefinition();
            if (def != null && def.equals(SWEConstants.DEF_SAMPLING_TIME))
            {
                table.setConverter(propId, new Converter<String, String>() {
                    private static final long serialVersionUID = 1L;
                    DateTimeFormat dateFormat = new DateTimeFormat();
                    public String convertToPresentation(String value, Class<? extends String> targetType, Locale locale) throws ConversionException
                    {
                        return dateFormat.formatIso(Double.parseDouble(value), 0);
                    }
                    @Override
                    public String convertToModel(String value, Class<? extends String> targetType, Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException
                    {
                        return null;
                    }
                    @Override
                    public Class<String> getModelType()
                    {
                        return String.class;
                    }
                    @Override
                    public Class<String> getPresentationType()
                    {
                        return String.class;
                    }                        
                });
            }
            
            // prepare indexer for reading from datablocks
            indexers.add(new ScalarIndexer(recordDef, (ScalarComponent)component));
        }
        
        // call recursively for records
        else if (component instanceof DataRecord || component instanceof Vector)
        {
            for (int i = 0; i < component.getComponentCount(); i++)
            {
                DataComponent child = component.getComponent(i);
                addColumns(recordDef, child, table, indexers);
            }
        }
    }
    
    
    protected String getPrettyName(DataComponent dataComponent)
    {
        String label = dataComponent.getLabel();
        if (label == null)
            label = DisplayUtils.getPrettyName(dataComponent.getName());
        return label;
    }
    
    
    protected Panel newPanel(String title)
    {
        Panel panel = new Panel(title);
        GridLayout layout = new GridLayout(2, 2);
        layout.setWidth(100.0f, Unit.PERCENTAGE);
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setColumnExpandRatio(0, 0.2f);
        layout.setColumnExpandRatio(1, 0.8f);
        layout.setDefaultComponentAlignment(Alignment.TOP_LEFT);
        panel.setContent(layout);
        return panel;
    }
}
