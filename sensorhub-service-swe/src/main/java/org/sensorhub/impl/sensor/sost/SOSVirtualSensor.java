/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.sost;

import java.util.HashMap;
import java.util.Map;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.DataInterface;
import net.opengis.swe.v20.AbstractSWEIdentifiable;
import net.opengis.swe.v20.BinaryBlock;
import net.opengis.swe.v20.BinaryComponent;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.BinaryMember;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataStream;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.BinaryComponentImpl;
import org.vast.data.DataIterator;
import org.vast.ogc.om.IObservation;
import org.vast.ows.sos.ISOSDataConsumer;


/**
 * <p>
 * Virtual sensor interface created by SOS InsertSensor
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 2, 2014
 */
public class SOSVirtualSensor extends AbstractSensorModule<SOSVirtualSensorConfig> implements ISOSDataConsumer
{
    protected static final Logger log = LoggerFactory.getLogger(SOSVirtualSensor.class);
    
    Map<DataStructureHash, String> structureToOutputMap = new HashMap<DataStructureHash, String>();
    
    
    // utility class to compute data component hashcode
    class DataStructureHash
    {
        int hashcode;
        
        public DataStructureHash(DataComponent comp, DataEncoding enc)
        {
            hashcode = computeDataStructureHashCode(comp, enc);
        }        
        
        @Override
        public int hashCode()
        {
            return hashcode;
        }        
    }
    
    
    public SOSVirtualSensor()
    {
    }


    @Override
    public void newObservation(IObservation... observations) throws Exception
    {
        // TODO Auto-generated method stub

        // also register template
    }


    @Override
    public String newResultTemplate(DataComponent component, DataEncoding encoding)
    {
        // TODO check if template is compatible with sensor description outputs?        
        // TODO merge all templates with same structure but different encodings to the same output
        
        // try to obtain corresponding data interface
        DataStructureHash hashObj = new DataStructureHash(component, encoding);
        String templateID = structureToOutputMap.get(hashObj);
                
        // create a new one if needed
        if (templateID == null)
        {        
            SOSVirtualSensorOutput newOutput = new SOSVirtualSensorOutput(this, component, encoding);
            templateID = config.sensorUID + "-" + Integer.toHexString(hashObj.hashCode());
            component.setName(templateID);
            addOutput(newOutput, false);
            structureToOutputMap.put(hashObj, templateID);
        }
        
        return templateID;
    }
    
    
    /*
     * Computes simple hash code for reusing similar templates
     */
    private int computeDataStructureHashCode(DataComponent comp, DataEncoding enc)
    {
        StringBuilder buf = new StringBuilder();
        
        DataIterator it = new DataIterator(comp);
        while (it.hasNext())
        {
            comp = it.next();
            
            buf.append(comp.getName());
            buf.append('|');
            
            buf.append(comp.getClass().getSimpleName());
            buf.append('|');
            
            String defUri = comp.getDefinition();
            if (defUri != null)
            {
                buf.append('|');
                buf.append(defUri);
            }
            
            buf.append('\n');
        }
        
        if (enc != null)
        {
            buf.append(enc.getClass().getSimpleName());
            if (enc instanceof BinaryEncoding)
            {
                for (BinaryMember opts: ((BinaryEncoding) enc).getMemberList())
                {
                    buf.append(opts.getRef());
                    buf.append('|');
                    if (opts instanceof BinaryComponent)
                    {
                        buf.append(((BinaryComponentImpl)opts).getCdmDataType());
                    }
                    else if (opts instanceof BinaryBlock)
                    {
                        buf.append(((BinaryBlock)opts).getCompression());
                        buf.append('|');
                        buf.append(((BinaryBlock)opts).getEncryption());
                    }
                }
            }
        }
        
        return buf.toString().hashCode();
    }


    @Override
    public void newResultRecord(String templateID, DataBlock... dataBlocks) throws Exception
    {
        SOSVirtualSensorOutput output = (SOSVirtualSensorOutput)getObservationOutputs().get(templateID);
        log.debug("New record received for output " + output.getName());
        
        for (DataBlock dataBlock: dataBlocks)
            output.publishNewRecord(dataBlock);
    }


    @Override
    public void start() throws SensorHubException
    {
        // generate output interfaces from description
        for (AbstractSWEIdentifiable output: getCurrentSensorDescription().getOutputList())
        {
            DataComponent dataStruct = null;
            DataEncoding dataEnc = null;
            
            if (output instanceof DataStream)
            {
                dataStruct = ((DataStream) output).getElementType();
                dataEnc = ((DataStream) output).getEncoding();
                newResultTemplate(dataStruct, dataEnc);
            }
            else if (output instanceof DataInterface)
            {
                dataStruct = ((DataInterface) output).getData().getElementType();
                dataEnc = ((DataInterface) output).getData().getEncoding();
                newResultTemplate(dataStruct, dataEnc);
            }
        }
    }


    @Override
    public void stop() throws SensorHubException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public boolean isSensorDescriptionUpdateSupported()
    {
        return true;
    }


    @Override
    public void updateSensorDescription(AbstractProcess systemDesc, boolean recordHistory) throws SensorException
    {
        sensorDescription = systemDesc;
        long unixTime = System.currentTimeMillis();
        lastUpdatedSensorDescription = unixTime / 1000.;
        eventHandler.publishEvent(new SensorEvent(unixTime, getLocalID(), SensorEvent.Type.SENSOR_CHANGED));
    }


    @Override
    public boolean isConnected()
    {
        return true;
    }


    @Override
    public void updateSensor(AbstractProcess newSensorDescription) throws Exception
    {
        updateSensorDescription(newSensorDescription, false);        
    }

}
