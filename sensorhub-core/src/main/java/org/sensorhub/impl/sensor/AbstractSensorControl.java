/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor;

import java.util.List;
import java.util.UUID;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.utils.MsgUtils;
import org.vast.util.DateTime;


/**
 * <p>
 * Default implementation of common sensor control interface API methods.
 * By default, async exec, scheduling and status history are reported as
 * unsupported.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <SensorType> Type of parent sensor
 * @since Nov 22, 2014
 */
public abstract class AbstractSensorControl<SensorType extends ISensorModule<?>> implements ISensorControlInterface
{
    protected static String ERROR_NO_ASYNC = "Asynchronous command processing is not supported by driver ";
    protected static String ERROR_NO_SCHED = "Command scheduling is not supported by driver ";
    protected static String ERROR_NO_STATUS_HISTORY = "Status history is not supported by driver ";
    protected SensorType parentSensor;
    protected IEventHandler eventHandler;
    
    
    public AbstractSensorControl(SensorType parentSensor)
    {
        this.parentSensor = parentSensor;
        
        // obtain an event handler for this control input
        String moduleID = parentSensor.getLocalID();
        String topic = getName();
        this.eventHandler = SensorHub.getInstance().getEventBus().registerProducer(moduleID, topic);
    }
    
    
    @Override
    public ISensorModule<?> getParentSensor()
    {
        return parentSensor;
    }


    @Override
    public boolean isEnabled()
    {
        return true;
    }
    
    
    @Override
    public CommandStatus execCommandGroup(List<DataBlock> commands) throws SensorException
    {
        CommandStatus groupStatus = new CommandStatus();
        groupStatus.id = UUID.randomUUID().toString();
        groupStatus.status = StatusCode.COMPLETED;
        
        // if any of the commands fail, return fail status with
        // error message and don't process more commands
        for (DataBlock cmd: commands)
        {
            CommandStatus cmdStatus = execCommand(cmd);
            if (cmdStatus.status == StatusCode.REJECTED || cmdStatus.status == StatusCode.FAILED)
            {
                groupStatus.status = cmdStatus.status;
                groupStatus.message = cmdStatus.message;
                break;
            }          
        }
        
        return groupStatus;
    }


    @Override
    public boolean isAsyncExecSupported()
    {
        return false;
    }


    @Override
    public boolean isSchedulingSupported()
    {
        return false;
    }


    @Override
    public boolean isStatusHistorySupported()
    {
        return false;
    }


    @Override
    public CommandStatus sendCommand(DataBlock command) throws SensorException
    {
        throw new SensorException(ERROR_NO_ASYNC + MsgUtils.moduleClassAndId(parentSensor));
    }


    @Override
    public CommandStatus sendCommandGroup(List<DataBlock> commands) throws SensorException
    {
        throw new SensorException(ERROR_NO_ASYNC + MsgUtils.moduleClassAndId(parentSensor));
    }


    @Override
    public CommandStatus scheduleCommand(DataBlock command, DateTime execTime) throws SensorException
    {
        throw new SensorException(ERROR_NO_SCHED + MsgUtils.moduleClassAndId(parentSensor));
    }


    @Override
    public CommandStatus scheduleCommandGroup(List<DataBlock> commands, DateTime execTime) throws SensorException
    {
        throw new SensorException(ERROR_NO_SCHED + MsgUtils.moduleClassAndId(parentSensor));
    }


    @Override
    public CommandStatus cancelCommand(String commandID) throws SensorException
    {
        throw new SensorException(ERROR_NO_ASYNC + MsgUtils.moduleClassAndId(parentSensor));
    }


    @Override
    public CommandStatus getCommandStatus(String commandID) throws SensorException
    {
        throw new SensorException(ERROR_NO_ASYNC + MsgUtils.moduleClassAndId(parentSensor));
    }


    @Override
    public List<CommandStatus> getCommandStatusHistory(String commandID) throws SensorException
    {
        throw new SensorException(ERROR_NO_STATUS_HISTORY + MsgUtils.moduleClassAndId(parentSensor));
    }

    
    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }
    
}
