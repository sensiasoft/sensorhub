/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;

import java.io.Serializable;


/**
 * <p>
 * Base class for all sensor hub events
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 5, 2010
 */
public class Event implements Serializable
{
    private static final long serialVersionUID = 9176636296502966036L;
    
    protected Object source;
    protected long timeStamp;
    protected int code;
    protected String description;
    
    
    public Object getSource()
    {
        return source;
    }


    public void setSource(Object source)
    {
        this.source = source;
    }


    public long getTimeStamp()
    {
        return timeStamp;
    }


    public void setTimeStamp(long timeStamp)
    {
        this.timeStamp = timeStamp;
    }


    public int getCode()
    {
        return code;
    }
    
    
    public void setCode(int code)
    {
        this.code = code;
    }
    
    
    public String getDescription()
    {
        return description;
    }
    
    
    public void setDescription(String description)
    {
        this.description = description;
    }
    
}
