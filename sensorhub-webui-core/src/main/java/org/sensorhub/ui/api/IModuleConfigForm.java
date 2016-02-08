/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui.api;

import java.util.Map;
import org.sensorhub.ui.data.ComplexProperty;
import org.sensorhub.ui.data.MyBeanItem;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.ui.ComponentContainer;


/**
 * <p>
 * Interface for all UI forms used to configure modules
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since 0.5
 */
public interface IModuleConfigForm extends ComponentContainer
{
    
    /**
     * Builds the whole form for a complex property (i.e. whose value is an object)
     * @param propertyId
     * @param prop
     */
    public void build(String propertyId, ComplexProperty prop);
    
    
    /**
     * Builds the whole form for a given config object.
     * @param title title of the form
     * @param popupText help popup text shown when hovering on the form title
     * @param beanItem object to render a form for
     */
    public void build(String title, String popupText, MyBeanItem<? extends Object> beanItem);
    
    
    /**
     * Get the class whose allowed bean types should derive from
     * @return parent type or null if the bean cannot be replaced by another type 
     */
    public Class<?> getPolymorphicBeanParentType();
    
    
    /**
     * Return possible object types for the given property ID
     * @param propId property ID
     * @return map of names to types assignable to that property
     */
    public Map<String, Class<?>> getPossibleTypes(String propId);
    
    
    /**
     * Commit all changes made in the UI values to the underlying bean object
     * @throws CommitException if data cannot be committed
     */
    public void commit() throws CommitException;
    
}
