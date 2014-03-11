/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.sost;

import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.persistence.ISensorDescriptionStorage;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorInterface;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.SensorHub;
import org.vast.cdm.common.BinaryBlock;
import org.vast.cdm.common.BinaryComponent;
import org.vast.cdm.common.BinaryEncoding;
import org.vast.cdm.common.BinaryOptions;
import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.cdm.common.DataEncoding;
import org.vast.data.DataIterator;
import org.vast.ogc.om.IObservation;
import org.vast.ows.sos.ISOSDataConsumer;
import org.vast.sensorML.SMLProcess;
import org.vast.sweCommon.SweConstants;
import org.vast.util.DateTime;


/**
 * <p>
 * Virtual sensor interface created by SOS InsertSensor
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Mar 2, 2014
 */
public class SOSVirtualSensor implements ISOSDataConsumer, ISensorInterface<SOSVirtualSensorConfig>
{
    SOSVirtualSensorConfig config;
    ISensorDescriptionStorage<?> smlStorage;
    Map<String, SOSVirtualSensorOutput> outputs = new HashMap<String, SOSVirtualSensorOutput>();
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
    public void updateSensor(SMLProcess newSensorDescription) throws Exception
    {
        smlStorage.update(newSensorDescription);
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
        
        
        // try to otbain corresponding data interface
        DataStructureHash hashObj = new DataStructureHash(component, encoding);
        String templateID = structureToOutputMap.get(hashObj);
        
        // create a new one if needed
        if (templateID == null)
        {        
            SOSVirtualSensorOutput newOutput = new SOSVirtualSensorOutput(this, component, encoding);
            templateID = config.sensorUID + "-" + Integer.toHexString(hashObj.hashCode());
            outputs.put(templateID, newOutput);
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
            
            QName compQName = (QName)comp.getProperty(SweConstants.COMP_QNAME);
            if (compQName != null)
                buf.append(compQName.toString());
            else
                buf.append(comp.getClass().getSimpleName());
            buf.append('|');
            
            String defUri = (String)comp.getProperty(SweConstants.DEF_URI);
            if (defUri != null)
            {
                buf.append('|');
                buf.append(defUri);
            }
            
            buf.append('\n');
        }
        
        if (enc != null)
        {
            buf.append(enc.getEncodingType());
            if (enc instanceof BinaryEncoding)
            {
                for (BinaryOptions opts: ((BinaryEncoding)enc).componentEncodings)
                {
                    buf.append(opts.componentName);
                    buf.append('|');
                    if (opts instanceof BinaryComponent)
                        buf.append(((BinaryComponent)opts).type);
                    else if (opts instanceof BinaryBlock)
                    {
                        buf.append(((BinaryBlock)opts).compression);
                        buf.append('|');
                        buf.append(((BinaryBlock)opts).encryption);
                    }
                }
            }
        }
        
        return buf.toString().hashCode();
    }


    @Override
    public void newResultRecord(String templateID, DataBlock... dataBlocks) throws Exception
    {
        for (DataBlock dataBlock: dataBlocks)
            outputs.get(templateID).publishNewRecord(dataBlock);
    }
    
    
    @Override
    public boolean isEnabled()
    {
        return config.enabled;
    }


    @Override
    public void init(SOSVirtualSensorConfig config) throws SensorHubException
    {
        this.config = config;
    }


    @Override
    public void updateConfig(SOSVirtualSensorConfig config) throws SensorHubException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public void start() throws SensorHubException
    {
        smlStorage = SensorHub.getInstance().getPersistenceManager().getSensorDescriptionStorage();
        
        // generate output interfaces from description
        DataComponent outputList = getCurrentSensorDescription().getOutputList();
        int numOutputs = outputList.getComponentCount();
        for (int i=0; i<numOutputs; i++)
        {
            DataComponent dataStruct = outputList.getComponent(i);
            DataEncoding dataEnc = (DataEncoding)dataStruct.getProperty(SweConstants.ENCODING_TYPE);
            newResultTemplate(dataStruct, dataEnc);
        }
    }


    @Override
    public void stop() throws SensorHubException
    {
        // TODO Auto-generated method stub
    }


    @Override
    public SOSVirtualSensorConfig getConfiguration()
    {
        return config;
    }


    @Override
    public String getName()
    {
        return config.name;
    }


    @Override
    public String getLocalID()
    {
        return config.id;
    }


    @Override
    public void saveState(IModuleStateSaver saver) throws SensorHubException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void loadState(IModuleStateLoader loader) throws SensorHubException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void cleanup() throws SensorHubException
    {
        // TODO Auto-generated method stub

    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        // TODO Auto-generated method stub

    }


    @Override
    public boolean isSensorDescriptionUpdateSupported()
    {
        return true;
    }


    @Override
    public boolean isSensorDescriptionHistorySupported()
    {
        return true;
    }


    @Override
    public SMLProcess getCurrentSensorDescription() throws SensorException
    {
        return smlStorage.getSensorDescription(config.sensorUID);
    }


    @Override
    public SMLProcess getSensorDescription(DateTime t) throws SensorException
    {
        return smlStorage.getSensorDescriptionAtTime(config.sensorUID, t.getTime());
    }


    @Override
    public void updateSensorDescription(SMLProcess systemDesc, boolean recordHistory) throws SensorException
    {
        if (!recordHistory)
            smlStorage.removeHistory(systemDesc.getIdentifier());
        
        smlStorage.update(systemDesc);
    }


    @Override
    public Map<String, SOSVirtualSensorOutput> getAllOutputs() throws SensorException
    {
        return outputs;
    }


    @Override
    public Map<String, SOSVirtualSensorOutput> getStatusOutputs() throws SensorException
    {
        return null;
    }


    @Override
    public Map<String, SOSVirtualSensorOutput> getObservationOutputs() throws SensorException
    {
        return outputs;
    }


    @Override
    public Map<String, ? extends ISensorControlInterface> getCommandInputs() throws SensorException
    {
        return null;
    }


    @Override
    public boolean isConnected()
    {
        return true;
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        // TODO Auto-generated method stub

    }

}
