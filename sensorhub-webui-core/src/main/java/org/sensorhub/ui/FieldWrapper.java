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

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Field;


public abstract class FieldWrapper<T extends Object> extends CustomField<T>
{
    private static final long serialVersionUID = 4400545338126007506L;
    Field<T> innerField;
    
    
    public FieldWrapper(Field<T> innerField)
    {
        this.innerField = innerField;
        this.setCaption(innerField.getCaption());
        innerField.setCaption(null);
    }    
    

    @Override
    public Class<? extends T> getType()
    {
        return innerField.getType();
    }   


    @Override
    public void commit() throws SourceException, InvalidValueException
    {
        innerField.commit();
    }


    @Override
    public void discard() throws SourceException
    {
        innerField.discard();
    }


    @Override
    protected T getInternalValue()
    {
        return innerField.getValue();
    }


    @Override
    public void setValue(T newFieldValue) throws com.vaadin.data.Property.ReadOnlyException, ConversionException
    {
        innerField.setValue(newFieldValue);
        markAsDirty();
    }
}
