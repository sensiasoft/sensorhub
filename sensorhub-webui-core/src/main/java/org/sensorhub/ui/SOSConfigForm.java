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

import java.util.LinkedHashMap;
import java.util.Map;
import org.sensorhub.api.persistence.IStorageModule;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.data.Property;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Field;
import com.vaadin.ui.Link;


public class SOSConfigForm extends GenericConfigForm
{
    private static final long serialVersionUID = -5570947777524310604L;
    protected static final String SOS_PACKAGE = "org.sensorhub.impl.service.sos.";
    protected static final String PROP_DATAPROVIDERS = "dataProviders";
    protected static final String PROP_ENDPOINT = "endPoint";
    protected static final String PROP_URI = ".uri";
    protected static final String PROP_STORAGEID = ".storageID";
    protected static final String PROP_SENSORID = ".sensorID";
    protected static final String PROP_PROCESSID = ".processID";
    
    
    @Override
    public void build(String title, MyBeanItem<? extends Object> beanItem)
    {
        super.build(title, beanItem);
        
        // add link to capabilities
        Property<?> endPointProp = beanItem.getItemProperty(PROP_ENDPOINT);
        if (endPointProp != null)
        {
            String baseUrl = ((String)endPointProp.getValue()).substring(1);
            String href = baseUrl + "?service=SOS&version=2.0&request=GetCapabilities";
            Link link = new Link("Link to capabilities", new ExternalResource(href), "_blank", 0, 0, null);
            this.addComponent(link, 0);
        }
    }
    
    
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop)
    {
        Field<Object> field = (Field<Object>)super.buildAndBindField(label, propId, prop);
        
        if (propId.endsWith(PROP_ENDPOINT))
        {
            field.addValidator(new StringLengthValidator(MSG_REQUIRED_FIELD, 1, 256, false));
        }
        else if (propId.endsWith(PROP_ENABLED))
        {
            field.setVisible(true);
        }
        else if (propId.endsWith(PROP_URI))
        {
            field.addValidator(new StringLengthValidator(MSG_REQUIRED_FIELD, 1, 256, false));
        }
        else if (propId.endsWith(PROP_STORAGEID))
        {
            field = makeModuleSelectField(field, IStorageModule.class);
        }
        else if (propId.endsWith(PROP_SENSORID))
        {
            field = makeModuleSelectField(field, ISensorModule.class);
            field.addValidator(new StringLengthValidator(MSG_REQUIRED_FIELD, 1, 256, false));
        }
        else if (propId.endsWith(PROP_DATAPROVIDERS + PROP_SEP + PROP_NAME))
            field.setVisible(true);
        
        return field;
    }


    @Override
    public Map<String, Class<?>> getPossibleTypes(Object propId)
    {
        if (propId.equals(PROP_DATAPROVIDERS))
        {
            Map<String, Class<?>> classList = new LinkedHashMap<String, Class<?>>();
            try
            {
                classList.put("Sensor Data Source", Class.forName(SOS_PACKAGE + "SensorDataProviderConfig"));
                classList.put("Stream Process Data Source", Class.forName(SOS_PACKAGE + "StreamProcessProviderConfig"));
                classList.put("Storage Data Source", Class.forName(SOS_PACKAGE + "StorageDataProviderConfig"));                
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
            return classList;
        }
        
        return super.getPossibleTypes(propId);
    }
}
