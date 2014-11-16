/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
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
