/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.sost.SOSVirtualSensor;
import org.vast.ogc.om.IObservation;
import org.vast.ows.sos.ISOSDataConsumer;


/**
 * <p>
 * Wrapper data consumer for updating only a virtual sensor
 * </p>
 *
 * @author Alex Robin>
 * @since Feb 13, 2015
 */
public class SensorDataConsumer implements ISOSDataConsumer
{
    SOSVirtualSensor sensor;
    
    
    public SensorDataConsumer(SensorConsumerConfig config) throws SensorHubException
    {
        ModuleRegistry moduleReg = SensorHub.getInstance().getModuleRegistry();
        this.sensor = (SOSVirtualSensor)moduleReg.getModuleById(config.sensorID);
    }
    
    
    @Override
    public void updateSensor(AbstractProcess newSensorDescription) throws Exception
    {
        sensor.updateSensorDescription(newSensorDescription, false);
    }


    @Override
    public void newObservation(IObservation... observations) throws Exception
    {
        sensor.newObservation(observations);
    }


    @Override
    public String newResultTemplate(DataComponent component, DataEncoding encoding) throws Exception
    {
        return sensor.newResultTemplate(component, encoding);
    }


    @Override
    public void newResultRecord(String templateID, DataBlock... dataBlocks) throws Exception
    {
        sensor.newResultRecord(templateID, dataBlocks);
    }


    @Override
    public Template getTemplate(String templateID) throws Exception
    {
        Template template = new Template();
        ISensorDataInterface output = sensor.getAllOutputs().get(templateID);
        template.component = output.getRecordDescription();
        template.encoding = output.getRecommendedEncoding();
        return template;
    }
}
