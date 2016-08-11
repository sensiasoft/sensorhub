/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.sensor;

import net.opengis.sensorml.v20.PhysicalSystem;
import net.opengis.sensorml.v20.SpatialFrame;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.sensorML.SMLFactory;


public class FakeSensorWithPos extends AbstractSensorModule<SensorConfigWithPos>
{
       
    
    public FakeSensorWithPos()
    {
        this.uniqueID = "urn:sensors:mysensor:001";
        this.xmlID = "SENSOR1";
    }
    
    
    public void setSensorUID(String sensorUID)
    {
        this.uniqueID = sensorUID;
    }
    
    
    public void setDataInterfaces(ISensorDataInterface... outputs) throws SensorException
    {
        for (ISensorDataInterface o: outputs)
            addOutput(o, false);
    }
    
    
    public void setControlInterfaces(ISensorControlInterface... inputs) throws SensorException
    {
        for (ISensorControlInterface i: inputs)
            addControlInput(i);
    }
    

    @Override
    public void updateConfig(SensorConfigWithPos config) throws SensorHubException
    {
    }


    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            
            SMLFactory smlFac = new SMLFactory();
            SpatialFrame localRefFrame = smlFac.newSpatialFrame();
            localRefFrame.setId(getLocalFrameID());
            localRefFrame.setOrigin("Frame Origin");
            localRefFrame.addAxis("X", "The X axis");
            localRefFrame.addAxis("Y", "The Y axis");
            localRefFrame.addAxis("Z", "The Z axis");
            ((PhysicalSystem)sensorDescription).addLocalReferenceFrame(localRefFrame);
        }
    }


    @Override
    public void start() throws SensorHubException
    {
        for (ISensorDataInterface o: getObservationOutputs().values())
            ((IFakeSensorOutput)o).start();
    }
    
    
    @Override
    public void stop() throws SensorHubException
    {
        for (ISensorDataInterface o: getObservationOutputs().values())
            ((IFakeSensorOutput)o).stop();
    }


    @Override
    public boolean isConnected()
    {
        return true;
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {
    }
    
    
    public void setStartedState()
    {
        setState(ModuleState.STARTED);
    }
}
