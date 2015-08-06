/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui.data;

import java.lang.reflect.Field;
import org.sensorhub.api.config.DisplayInfo;
import com.vaadin.data.util.AbstractProperty;


@SuppressWarnings({ "serial" })
public abstract class BaseProperty<T> extends AbstractProperty<T>
{
    protected Field f;
    
    
    public BaseProperty()
    {
        super();
    }

    
    public String getLabel()
    {
        DisplayInfo ann = f.getAnnotation(DisplayInfo.class);
        if (ann != null && ann.label().length() > 0)
            return ann.label();
        else
            return null;
    }

    
    public String getDescription()
    {
        DisplayInfo ann = f.getAnnotation(DisplayInfo.class);
        if (ann != null && ann.desc().length() > 0)
            return ann.desc();
        else
            return null;
    }

}