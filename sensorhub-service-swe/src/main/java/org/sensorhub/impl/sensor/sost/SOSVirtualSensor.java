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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import net.opengis.OgcProperty;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.TimePeriod;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.DataInterface;
import net.opengis.sensorml.v20.IOPropertyList;
import net.opengis.sensorml.v20.PhysicalSystem;
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
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.sensorhub.utils.MsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.BinaryComponentImpl;
import org.vast.data.DataIterator;
import org.vast.data.SWEFactory;
import org.vast.ogc.om.IObservation;
import org.vast.sensorML.SMLUtils;


/**
 * <p>
 * Virtual sensor interface created by SOS InsertSensor
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 2, 2014
 */
public class SOSVirtualSensor extends AbstractSensorModule<SOSVirtualSensorConfig>
{
    protected static final Logger log = LoggerFactory.getLogger(SOSVirtualSensor.class);
    
    Map<DataStructureHash, String> structureToTemplateIdMap = new HashMap<DataStructureHash, String>();
    Map<DataStructureHash, String> structureToOutputMap = new HashMap<DataStructureHash, String>();
    AbstractFeature currentFoi;
    
    
    // utility class to compute data component hashcode
    class DataStructureHash
    {
        private int hashcode;
        
        public DataStructureHash(DataComponent comp, DataEncoding enc)
        {
            hashcode = computeDataStructureHashCode(comp, enc);
        }        
        
        @Override
        public int hashCode()
        {
            return hashcode;
        }
        
        @Override
        public boolean equals(Object obj)
        {
            if (hashcode == ((DataStructureHash)obj).hashcode)
                return true;
            return false;
        }
    }
    
    
    public SOSVirtualSensor()
    {
    }
    
    
    @Override
    public String getName()
    {
        if (sensorDescription != null && sensorDescription.getName() != null)
            return sensorDescription.getName();
        
        return config.name;
    }
    
    
    @Override
    public AbstractFeature getCurrentFeatureOfInterest()
    {
        return currentFoi;
    }


    public void newObservation(IObservation... observations) throws Exception
    {
        // TODO Auto-generated method stub

        // also register template
    }


    public String newResultTemplate(DataComponent component, DataEncoding encoding)
    {
        return newResultTemplate(component, encoding, null);
    }
    
    
    public String newResultTemplate(DataComponent component, DataEncoding encoding, IObservation obsTemplate)
    {
        // TODO merge all templates with same structure but different encodings to the same output
        
        // try to obtain corresponding data interface
        DataStructureHash templateHashObj = new DataStructureHash(component, encoding);
        String templateID = structureToTemplateIdMap.get(templateHashObj);
                
        // create a new one if needed
        if (templateID == null)
        {
            // use SensorML output name if structure matches one of the outputs
            DataStructureHash outputHashObj = new DataStructureHash(component, null);
            String outputName = structureToOutputMap.get(outputHashObj);
                        
            // else generate output name
            if (outputName == null)
                outputName = "output" + getAllOutputs().size();
            
            // add sensor output interface
            SOSVirtualSensorOutput newOutput = new SOSVirtualSensorOutput(this, component, encoding);
            templateID = generateTemplateID(outputName);
            component.setName(outputName);
            addOutput(newOutput, false);
            structureToTemplateIdMap.put(templateHashObj, templateID);
            
            // also update sensor description with data stream to keep encoding definition
            if (sensorDescription != null)
            {
                DataStream ds = new SWEFactory().newDataStream();
                ds.setElementType(outputName, component);
                ds.setEncoding(encoding);
                
                OgcProperty<AbstractSWEIdentifiable> output = sensorDescription.getOutputList().getProperty(outputName);
                if (output == null)
                    sensorDescription.addOutput(outputName, ds);
                else if (!(output.getValue() instanceof DataStream))
                    output.setValue(ds);
            }
        }        
        
        return templateID;
    }
    
    
    public void newFeatureOfInterest(String templateID, IObservation obsTemplate)
    {
        // process feature of interest
        if (obsTemplate != null)
        {
            String outputName = getOutputNameFromTemplateID(templateID);
            SOSVirtualSensorOutput output = (SOSVirtualSensorOutput)getAllOutputs().get(outputName);
            
            currentFoi = obsTemplate.getFeatureOfInterest();
            if (currentFoi != null)
                output.publishNewFeatureOfInterest(currentFoi);
        }
    }
    
    
    /*
     * Computes simple hash code for reusing similar templates
     */
    private int computeDataStructureHashCode(DataComponent comp, DataEncoding enc)
    {
        StringBuilder buf = new StringBuilder();
        
        boolean root = true;
        DataIterator it = new DataIterator(comp);
        while (it.hasNext())
        {
            comp = it.next();
            
            // skip root name because it's not always set
            if (!root)
            {
                buf.append(comp.getName());
                buf.append('|');
            }
            root = false;
                        
            buf.append(comp.getClass().getSimpleName());
                        
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
                    buf.append('|');
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


    public void newResultRecord(String templateID, DataBlock... dataBlocks) throws Exception
    {
        String outputName = getOutputNameFromTemplateID(templateID);
        
        SOSVirtualSensorOutput output = (SOSVirtualSensorOutput)getObservationOutputs().get(outputName);
        log.trace("New record received for output " + output.getName());
        
        for (DataBlock dataBlock: dataBlocks)
            output.publishNewRecord(dataBlock);
    }
    
    
    public final String getOutputNameFromTemplateID(String templateID)
    {
        return templateID.substring(templateID.lastIndexOf('#')+1);
    }
    
    
    public final String generateTemplateID(String outputName)
    {
        return config.id + '#' + outputName;
    }


    @Override
    public void start() throws SensorHubException
    {        
    }


    @Override
    public void stop() throws SensorHubException
    {
    }


    @Override
    public void cleanup() throws SensorHubException
    {
        File f = new File(this.getLocalID() + ".xml");
        if (f.exists())
            f.delete();
    }


    @Override
    public boolean isSensorDescriptionUpdateSupported()
    {
        return true;
    }


    @Override
    protected void updateSensorDescription()
    {
        sensorDescription.setUniqueIdentifier(config.sensorUID);
        
        // don't do anything more here.
        // we wait until description is set by SOS consumer
    }
    
    
    @Override
    public void updateSensorDescription(AbstractProcess systemDesc, boolean recordHistory) throws SensorException
    {
        sensorDescription = systemDesc;
        
        // generate output hashcodes to compare with insert result templates
        structureToOutputMap.clear();
        IOPropertyList outputList = sensorDescription.getOutputList();
        for (int i = 0; i  < outputList.size(); i++)
        {
            DataStructureHash hashObj = new DataStructureHash(outputList.getComponent(i), null);
            structureToOutputMap.put(hashObj, outputList.getProperty(i).getName());
        }
        
        long unixTime = System.currentTimeMillis();
        lastUpdatedSensorDescription = unixTime;
        eventHandler.publishEvent(new SensorEvent(unixTime, this, SensorEvent.Type.SENSOR_CHANGED));
    }
    
    
    /*
     * Set sensor description when reviving from storage (w/o sending event)
     */
    public void setSensorDescription(AbstractProcess systemDesc)
    {
        sensorDescription = systemDesc;
        long unixTime = System.currentTimeMillis();
        lastUpdatedSensorDescription = unixTime;
    }


    @Override
    public boolean isConnected()
    {
        // TODO use timeout value
        /*long now = System.currentTimeMillis();
        
        for (ISensorDataInterface output: this.getAllOutputs().values())
        {
            double samplingPeriod = output.getAverageSamplingPeriod();
            if (now - output.getLatestRecordTime() < 10*samplingPeriod)
                return true;
        }
        
        return false;*/
        return true;
    }


    @Override
    public void saveState(IModuleStateManager saver) throws SensorHubException
    {
        try
        {
            File f = new File(this.getLocalID() + ".xml");
            if (sensorDescription != null)
                new SMLUtils(SMLUtils.V2_0).writeProcess(new FileOutputStream(f), sensorDescription, true);
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error while saving state for module " + MsgUtils.moduleString(this), e);
        }
    }


    @Override
    public void loadState(IModuleStateManager loader) throws SensorHubException
    {
        try
        {
            File f = new File(this.getLocalID() + ".xml");
            if (f.exists())
            {
                sensorDescription = (PhysicalSystem)new SMLUtils(SMLUtils.V2_0).readProcess(new FileInputStream(f));
                int timeListSize = sensorDescription.getValidTimeList().size();
                if (timeListSize > 0)
                {
                    double begin = ((TimePeriod)sensorDescription.getValidTimeList().get(0)).getBeginPosition().getDecimalValue();
                    lastUpdatedSensorDescription = (long)(begin*1000);
                }
                
                // generate output interfaces from description
                for (AbstractSWEIdentifiable output: sensorDescription.getOutputList())
                {
                    DataComponent dataStruct = null;
                    DataEncoding dataEnc = null;
                    
                    if (output instanceof DataStream)
                    {
                        dataStruct = ((DataStream) output).getElementType();
                        dataEnc = ((DataStream) output).getEncoding();   
                    }
                    else if (output instanceof DataInterface)
                    {
                        dataStruct = ((DataInterface) output).getData().getElementType();
                        dataEnc = ((DataInterface) output).getData().getEncoding();                        
                    }
                    
                    if (dataStruct != null && dataEnc != null)
                    {
                        // register output hashcode
                        DataStructureHash hashObj = new DataStructureHash(dataStruct, null);
                        structureToOutputMap.put(hashObj, dataStruct.getName());
                        
                        // register as template
                        newResultTemplate(dataStruct, dataEnc);
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new SensorHubException("Error while loading state for module " + MsgUtils.moduleString(this), e);
        }
    }
}
