/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.sensor;

import net.opengis.swe.v20.AllowedTokens;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.data.AllowedTokensImpl;
import org.vast.data.CategoryImpl;


/**
 * <p>
 * Fake control input implementation for testing sensor control API
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jan 29, 2015
 */
public class FakeSensorControl2 extends AbstractSensorControl<FakeSensor> implements ISensorControlInterface
{
    String name;
    int counter = 1;
    
    
    public FakeSensorControl2(FakeSensor parentSensor)
    {
        super(parentSensor);
        this.name = "command2";
    }


    @Override
    public String getName()
    {
        return name;
    }


    @Override
    public DataComponent getCommandDescription()
    {
        Category c = new CategoryImpl();
        c.setName(name);
        c.setDefinition("urn:blabla:trigger");
        AllowedTokens tokens = new AllowedTokensImpl();
        tokens.addValue("NOW");
        tokens.addValue("REPEAT");
        tokens.addValue("STOP");
        c.setConstraint(tokens);                
        return c;
    }


    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
        return new CommandStatus(String.format("%03d",  counter++), StatusCode.COMPLETED);
    }

}
