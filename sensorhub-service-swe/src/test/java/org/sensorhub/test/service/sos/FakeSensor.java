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

package org.sensorhub.test.service.sos;

import net.opengis.sensorml.v20.AbstractProcess;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;


public class FakeSensor extends AbstractSensorModule<SensorConfig>
{
  
    
    public FakeSensor()
    {        
    }
    
    
    public void setDataInterfaces(ISensorDataInterface... outputs) throws SensorException
    {
        for (ISensorDataInterface o: outputs)
            addOutput(o, false);
    }
    
    
    @Override
    public boolean isEnabled()
    {
        return config.enabled;
    }
    
    
    @Override
    public void init(SensorConfig config) throws SensorHubException
    {
        this.config = config;        
    }


    @Override
    public void updateConfig(SensorConfig config) throws SensorHubException
    {
    }
    
    
    @Override
    public AbstractProcess getCurrentSensorDescription() throws SensorException
    {
        AbstractProcess sml = super.getCurrentSensorDescription();
        sml.setUniqueIdentifier("urn:sensors:mysensor:001");        
        return sml;
    }
    
    
    @Override
    public void start()
    {
    }
    
    
    @Override
    public void stop()
    {
    }


    @Override
    public void cleanup() throws SensorHubException
    {
    }


    @Override
    public boolean isConnected()
    {
        return true;
    }
}
