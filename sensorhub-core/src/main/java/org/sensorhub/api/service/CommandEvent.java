/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.service;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;


/**
 * <p>
 * Type of event generated when new command are received by control services.
 * It is immutable and carries command data by reference
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
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
     * @param serviceID
     * @param commandID 
     * @param commandDescriptor
     * @param commandData
     */
    public CommandEvent(String serviceID, String commandID, DataComponent commandDescriptor, DataBlock commandData)
    {
        super(serviceID, EventType.COMMAND_AVAILABLE);
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
