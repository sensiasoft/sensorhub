/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.sensor;

import java.util.List;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.IEventProducer;
import org.vast.util.DateTime;


/**
 * <p>
 * Interface to be implemented by all sensor drivers connected to the system
 * Commands can be sent to each sensor controllable input via this interface.
 * Commands can be executed synchronously or asynchronously by sensors.
 * If asynchronous mode is supported, implementations of this class MUST produce
 * events of type SensorControlEvent.
 * </p>
 * 
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public interface ISensorControlInterface extends IEventProducer
{

    /**
     * Allows by-reference access to parent sensor interface
     * @return the parent sensor module instance
     */
    public ISensorModule<?> getParentSensor();
    
    
    /**
     * Gets the interface name. It should be the name reported in the map by
     * {@link org.sensorhub.api.sensor.ISensorModule#getCommandInputs()} 
     * @return name of this control interface
     */
    public String getName();


    /**
     * Checks if this interface is enabled
     * @return true if interface is enabled, false otherwise
     */
    public boolean isEnabled();


    /**
     * Checks asynchronous execution capability 
     * @return true if asynchronous command execution is supported, false otherwise
     */
    public boolean isAsyncExecSupported();


    /**
     * Checks scheduled execution capability 
     * @return true if scheduled command execution is supported, false otherwise
     */
    public boolean isSchedulingSupported();


    /**
     * Checks status history capability
     * @return true if status history is supported, false otherwise
     */
    public boolean isStatusHistorySupported();


    /**
     * Retrieves description of command message
     * Note that this can be a choice of multiple messages
     * @return Data component containing message structure
     */
    public DataComponent getCommandDescription();


    /**
     * Executes the command synchronously, blocking until completion of command
     * @param command command message data
     * @return status after execution of command
     * @throws SensorException
     */
    public CommandStatus execCommand(DataBlock command) throws SensorException;


    /**
     * Executes multiple commands synchronously and in the order specified.
     * This method will block until all commands are completed
     * @param commands list of command messages data
     * @return a single status message for the command group
     * @throws SensorException
     */
    public CommandStatus execCommandGroup(List<DataBlock> commands) throws SensorException;


    /**
     * Sends a command that will be executed asynchronously
     * @see #isAsyncExecSupported()
     * @param command command message data
     * @return initial status of the command (can change during the command life cycle)
     * @throws SensorException
     */
    public CommandStatus sendCommand(DataBlock command) throws SensorException;


    /**
     * Sends a group of commands for asynchronous execution.
     * Order is guaranteed but not atomicity
     * @see #isAsyncExecSupported()
     * @param commands list of command messages data
     * @return a single status object for the command group
     * @throws SensorException
     */
    public CommandStatus sendCommandGroup(List<DataBlock> commands) throws SensorException;


    /**
     * Schedules a command to be executed asynchronously at the specified time
     * @see #isSchedulingSupported()
     * @param command command message data
     * @param execTime desired time of execution
     * @return initial status of the command (can change during the command life cycle)
     * @throws SensorException
     */
    public CommandStatus scheduleCommand(DataBlock command, DateTime execTime) throws SensorException;


    /**
     * Schedules a group of commands to be executed asynchronously at the specified time.
     * Order is guaranteed but not atomicity
     * @see #isSchedulingSupported()
     * @param commands
     * @param execTime
     * @return a single status object for the command group
     * @throws SensorException
     */
    public CommandStatus scheduleCommandGroup(List<DataBlock> commands, DateTime execTime) throws SensorException;


    /**
     * Cancels a command before it is executed (for async or scheduled commands)
     * @see #isAsyncExecSupported()
     * @param commandID id of command to be canceled
     * @return status of the cancelled command
     * @throws SensorException
     */
    public CommandStatus cancelCommand(String commandID) throws SensorException;


    /**
     * Retrieves command status
     * @param commandID id of command to get status for
     * @see #isAsyncExecSupported()
     * @return current status of the command with the specified ID
     * @throws SensorException
     */
    public CommandStatus getCommandStatus(String commandID) throws SensorException;


    /**
     * Gets complete status history for the specified command
     * @see #isStatusHistorySupported()
     * @param commandID id of command to get status history for
     * @return list of command status, one object for each status change
     * @throws SensorException
     */
    public List<CommandStatus> getCommandStatusHistory(String commandID) throws SensorException;

}
