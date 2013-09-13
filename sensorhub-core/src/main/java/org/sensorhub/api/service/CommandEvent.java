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

package org.sensorhub.api.service;

import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;


/**
 * <p>
 * Type of event generated when new command are received by control services.
 * It is immutable and carries command data by reference
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 5, 2010
 */
public class CommandEvent extends ServiceEvent
{
    private static final long serialVersionUID = 6423151181007376051L;
    
    
    /**
     * ID of command associated to this event
     */
    protected String commandID;
    
    
    /**
     * Reference to command message descriptor
     */
    protected DataComponent commandDescriptor;
    
    
    /**
     * Reference to command message data 
     */
    protected DataBlock commandData;
    
    
    /**
     * Sole constructor
     * @param serviceId
     * @param commandDescriptor
     * @param commandData
     */
    public CommandEvent(String serviceId, String commandID, DataComponent commandDescriptor, DataBlock commandData)
    {
        super(serviceId, EventType.COMMAND_AVAILABLE);
        this.commandID = commandID;
        this.commandDescriptor = commandDescriptor;
        this.commandData = commandData;
    }


    /**
     * @return id of command that triggered the event
     */
    public String getCommandID()
    {
        return commandID;
    }


    /**
     * @return command message descriptor
     */
    public DataComponent getCommandDescriptor()
    {
        return commandDescriptor;
    }


    /**
     * @return command message data
     */
    public DataBlock getCommandData()
    {
        return commandData;
    }
}
