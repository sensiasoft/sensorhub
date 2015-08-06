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

import com.vaadin.data.Property;
import com.vaadin.data.Validator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.Field;


public class HttpServerConfigForm extends GenericConfigForm
{
    private static final long serialVersionUID = 3934416218769947436L;
    protected static final String PROP_SERVLET_ROOT = "servletsRootUrl";
    protected static final String PROP_HTTP_PORT = "httpPort";
    
    
    @Override
    protected Field<?> buildAndBindField(String label, String propId, Property<?> prop)
    {
        Field<?> field = super.buildAndBindField(label, propId, prop);
        
        if (propId.equals(PROP_SERVLET_ROOT))
        {
            field.addValidator(new StringLengthValidator(MSG_REQUIRED_FIELD, 2, 256, false));
        }
        else if (propId.equals(PROP_HTTP_PORT))
        {
            field.setWidth(100, Unit.PIXELS);
            //((TextField)field).getConverter().
            field.addValidator(new Validator() {
                private static final long serialVersionUID = 1L;
                public void validate(Object value) throws InvalidValueException
                {
                    int portNum = (Integer)value;
                    if (portNum > 10000 || portNum <= 80)
                        throw new InvalidValueException("Port number must be an integer number greater than 80 and lower than 10000");
                }
            });
        }
        
        return field;
    }
}
