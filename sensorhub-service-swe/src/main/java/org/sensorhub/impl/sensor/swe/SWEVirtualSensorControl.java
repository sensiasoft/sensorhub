/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.swe;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockInt;
import org.vast.data.DataBlockMixed;


public class SWEVirtualSensorControl extends AbstractSensorControl<SWEVirtualSensor>
{
    DataComponent cmdDescription;
    DataBlockMixed cmdWrapper;
    
    
    public SWEVirtualSensorControl(SWEVirtualSensor parentSensor, DataComponent cmdDescription)
    {
        this(parentSensor, cmdDescription, -1);
    }
    
    
    public SWEVirtualSensorControl(SWEVirtualSensor parentSensor, DataComponent cmdDescription, int choiceIndex)
    {
        super(parentSensor);
        this.cmdDescription = cmdDescription;
        
        if (choiceIndex >= 0)
        {
            cmdWrapper = new DataBlockMixed(2);
            DataBlockInt choiceIndexData = new DataBlockInt(1);
            choiceIndexData.setIntValue(choiceIndex);
            cmdWrapper.getUnderlyingObject()[0] = choiceIndexData;
        }
    }
    

    @Override
    public String getName()
    {
        return cmdDescription.getName();
    }
    

    @Override
    public DataComponent getCommandDescription()
    {
        return cmdDescription;
    }
    

    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
        try
        {
            // wrap to add choice index if several commands was advertised by server
            if (cmdWrapper != null)
            {
                cmdWrapper.getUnderlyingObject()[1] = (AbstractDataBlock)command;
                command = cmdWrapper;
            }            
            
            //SubmitResponse resp =
            parentSensor.spsClient.sendTaskMessage(command);
            
            // TODO handle SPS request and task status
            CommandStatus cmdStatus = new CommandStatus();
            cmdStatus.status = StatusCode.COMPLETED;
            
            return cmdStatus;
        }
        catch (SensorHubException e)
        {
            throw new SensorException("Error while sending command to SPS", e);
        }
    }
}
