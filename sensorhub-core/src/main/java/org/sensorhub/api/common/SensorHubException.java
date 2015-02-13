/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.common;


/**
 * <p>
 * Base exception class for all exceptions generated in SensorHub
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
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
