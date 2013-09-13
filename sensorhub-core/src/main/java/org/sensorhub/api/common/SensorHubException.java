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


/**
 * <p>
 * Base exception class for all exceptions generated in SensorHub
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 5, 2010
 */
public class SensorHubException extends Exception
{
    private static final long serialVersionUID = -5262139949248131452L;
    
    
    /**
     * Numerical code to allow polymorphism w/o subclassing
     * Code values are defined by concrete exception class
     */
    private int code;
    
    
    public SensorHubException(String message)
    {
        super(message);
    }


    public SensorHubException(String message, Throwable cause)
    {
        super(message, cause);
    }


    public SensorHubException(String message, int code, Throwable cause)
    {
        super(message, cause);
        this.code = code;
    }
    
    
    /**
     * @return exception code
     */
    public int getCode()
    {
        return code;
    }
}
