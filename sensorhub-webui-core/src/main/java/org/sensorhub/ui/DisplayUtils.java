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

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;


public class DisplayUtils
{

    public static String getPrettyName(String text)
    {
        StringBuilder buf = new StringBuilder(text.substring(text.lastIndexOf('.')+1));
        for (int i=0; i<buf.length()-1; i++)
        {
            char c = buf.charAt(i);
            
            if (i == 0)
            {
                char newcar = Character.toUpperCase(c);
                buf.setCharAt(i, newcar);
            }
                    
            else if (Character.isUpperCase(c) && Character.isLowerCase(buf.charAt(i+1)))
            {
                buf.insert(i, ' ');
                i++;
            }
        }
        
        return buf.toString();
    }
    
    
    public static void showOperationSuccessful(String text)
    {
        Notification notif = new Notification(
                "<span style=\"color:green\">" + FontAwesome.CHECK_CIRCLE_O.getHtml() +
                "</span>&nbsp;&nbsp;" + text, Notification.Type.WARNING_MESSAGE);
        notif.setHtmlContentAllowed(true);
        notif.show(UI.getCurrent().getPage());
    }
    
    
    public static void showUnauthorizedAccess(String text)
    {
        Notification notif = new Notification(
                "<span style=\"color:white\">" + FontAwesome.MINUS_CIRCLE.getHtml() +
                "</span>&nbsp;&nbsp;" + text, Notification.Type.ERROR_MESSAGE);
        notif.setHtmlContentAllowed(true);
        notif.show(UI.getCurrent().getPage());
    }
}
