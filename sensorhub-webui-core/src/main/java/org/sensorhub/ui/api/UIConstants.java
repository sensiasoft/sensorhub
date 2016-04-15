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

import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;


public interface UIConstants
{
    // constants for CSS styles
    public final static String STYLE_H1 = "h1";
    public final static String STYLE_H2 = "h2";
    public final static String STYLE_H3 = "h3";
    public final static String STYLE_COLORED = "colored";
    public final static String STYLE_SMALL = "small";
    public final static String STYLE_QUIET = "quiet";
    public final static String STYLE_SECTION_BUTTONS = "section-buttons";
    
    // constants for icon resources
    public static final Resource ADD_ICON = new ThemeResource("icons/add.gif");
    public static final Resource DEL_ICON = new ThemeResource("icons/remove.gif");
    public static final Resource LINK_ICON = new ThemeResource("icons/link.png");
    public static final Resource APPLY_ICON = new ThemeResource("icons/save.png");
    public static final Resource REFRESH_ICON = new ThemeResource("icons/refresh.gif");
    
    // constants for messages
    public static final String MSG_REQUIRED_FIELD = "Value is required";
    
    // constants for items property IDs 
    public static final char PROP_SEP = '.';
    public static final String PROP_ID = "id";
    public static final String PROP_NAME = "name";
    public static final String PROP_AUTOSTART = "autoStart";
    public static final String PROP_MODULECLASS = "moduleClass";
    
}
