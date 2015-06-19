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

import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.DataChoice;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.HasRefFrames;
import net.opengis.swe.v20.HasUom;
import net.opengis.swe.v20.SimpleComponent;
import net.opengis.swe.v20.Time;
import net.opengis.swe.v20.UnitReference;
import net.opengis.swe.v20.Vector;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;


public class SWECommonFormBuilder
{
    boolean showArrayTable;
    
    
    public Component buildForm(DataComponent dataComponent)
    {
        return buildWidget(dataComponent);
    }
    
    
    protected Component buildWidget(DataComponent dataComponent)
    {
        if (dataComponent instanceof DataRecord || dataComponent instanceof Vector)
        {
            VerticalLayout layout = new VerticalLayout();
            
            Label l = new Label();
            l.setValue(getCaption(dataComponent));
            l.setDescription(getTooltip(dataComponent));
            layout.addComponent(l);
            
            VerticalLayout form = new VerticalLayout();
            form.setMargin(new MarginInfo(false, false, false, true));
            form.setSpacing(false);
            for (int i = 0; i < dataComponent.getComponentCount(); i++)
            {
                DataComponent c = dataComponent.getComponent(i);
                Component w = buildWidget(c);
                if (w != null)
                    form.addComponent(w);
            }
            layout.addComponent(form);
            
            return layout;
        }
        
        else if (dataComponent instanceof DataArray)
        {
            DataArray dataArray = (DataArray)dataComponent;
            VerticalLayout layout = new VerticalLayout();
            
            Label l = new Label();
            l.setValue(getCaption(dataComponent));
            l.setDescription(getTooltip(dataComponent));
            layout.addComponent(l);
            
            VerticalLayout form = new VerticalLayout();
            form.setMargin(new MarginInfo(false, false, false, true));
            form.setSpacing(false);
            form.addComponent(buildWidget(dataArray.getElementType()));
            layout.addComponent(form);
            
            return layout;
        }
        
        else if (dataComponent instanceof DataChoice)
        {
            DataChoice dataChoice = (DataChoice)dataComponent;
            VerticalLayout layout = new VerticalLayout();
            
            Label l = new Label();
            l.setValue(getCaption(dataComponent));
            l.setDescription(getTooltip(dataComponent));
            layout.addComponent(l);
            
            return layout;
        }
        
        else if (dataComponent instanceof SimpleComponent)
        {
            Label c = new Label();
            c.setValue(getCaption(dataComponent));
            c.setDescription(getTooltip(dataComponent));
            return c;
        }
        
        return null;
    }
    
    
    protected String getCaption(DataComponent dataComponent)
    {
        StringBuffer caption = new StringBuffer();
        caption.append(getPrettyName(dataComponent));
        
        // uom code
        String uom = null;
        if (dataComponent instanceof HasUom)
        {
            UnitReference unit = ((HasUom) dataComponent).getUom();
              
            if (unit.hasHref() && unit.getHref().equals(Time.ISO_TIME_UNIT))
                uom = "ISO 8601";
            else
                uom = unit.getCode();          
                
            if (uom != null)
            {
                caption.append(" (");
                caption.append(uom);
                caption.append(')');
            }
        }
        
        // array size
        if (dataComponent instanceof DataArray)
        {
            caption.append(" [");
            caption.append(((DataArray)dataComponent).getComponentCount());
            caption.append(']');
        }
        
        return caption.toString();
    }
    
    
    protected String getTooltip(DataComponent dataComponent)
    {
        StringBuffer tooltip = new StringBuffer();
        
        if (dataComponent.getDescription() != null)
            tooltip.append("<p><b>Description:</b> ").append(dataComponent.getDescription()).append("</p>");
        
        String def = dataComponent.getDefinition();
        if (def != null)
            tooltip.append("<p><b>Definition:</b><a target='_blank' href='").append(def).append("'/>").append(def).append("</p>");
        
        if (dataComponent instanceof HasRefFrames)
        {
            String refFrame = ((HasRefFrames) dataComponent).getReferenceFrame();
            if (refFrame != null)
                tooltip.append("<p><b>Ref Frame:</b> ").append(refFrame).append("</p>");
            
            String localFrame = ((HasRefFrames) dataComponent).getLocalFrame();
            if (localFrame != null)
                tooltip.append("<p><b>Local Frame:</b> ").append(localFrame).append("</p>");
        }        
        
        return tooltip.toString();
    }
    
    
    protected String getPrettyName(DataComponent dataComponent)
    {
        String label = dataComponent.getLabel();
        if (label == null)
            label = DisplayUtils.getPrettyName(dataComponent.getName());
        return label;
    }
    
}
