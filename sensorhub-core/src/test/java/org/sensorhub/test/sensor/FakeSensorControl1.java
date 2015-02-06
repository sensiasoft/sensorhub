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

package org.sensorhub.test.sensor;

import net.opengis.swe.v20.AllowedTokens;
import net.opengis.swe.v20.Category;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Quantity;
import org.sensorhub.api.common.CommandStatus;
import org.sensorhub.api.common.CommandStatus.StatusCode;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorControl;
import org.vast.data.AllowedTokensImpl;
import org.vast.data.CategoryImpl;
import org.vast.data.DataRecordImpl;
import org.vast.data.QuantityImpl;


/**
 * <p>
 * Fake control input implementation for testing sensor control API
 * </p>
 *
 * <p>Copyright (c) 2014</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Jan 29, 2015
 */
public class FakeSensorControl1 extends AbstractSensorControl<FakeSensor> implements ISensorControlInterface
{
    String name;
    int counter = 1;
    
    
    public FakeSensorControl1(FakeSensor parentSensor)
    {
        super(parentSensor);
        this.name = "command1";
    }


    @Override
    public String getName()
    {
        return name;
    }


    @Override
    public DataComponent getCommandDescription()
    {
        DataComponent record = new DataRecordImpl(3);
        record.setName(this.name);
        record.setDefinition("urn:blabla:command");
        
        Quantity q = new QuantityImpl();
        q.setDefinition("urn:blabla:samplingPeriod");
        q.getUom().setCode("s");
        record.addComponent("samplingPeriod", q);
        
        Category c = new CategoryImpl();
        c.setDefinition("urn:blabla:sensitivity");
        AllowedTokens tokens = new AllowedTokensImpl();
        tokens.addValue("HIGH");
        tokens.addValue("LOW");
        c.setConstraint(tokens);
        record.addComponent("sens", c);
        
        return record;
    }


    @Override
    public CommandStatus execCommand(DataBlock command) throws SensorException
    {
        return new CommandStatus(String.format("%03d",  counter++), StatusCode.COMPLETED);
    }

}
