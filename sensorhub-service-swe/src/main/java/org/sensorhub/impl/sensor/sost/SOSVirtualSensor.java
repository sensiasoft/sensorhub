/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
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
import org.sensorhub.api.persistence.ISensorDescriptionStorage;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.vast.data.BinaryComponentImpl;
import org.vast.data.BinaryEncodingImpl;
import org.vast.data.DataIterator;
import org.vast.ogc.om.IObservation;
import org.vast.ows.sos.ISOSDataConsumer;
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
public class SOSVirtualSensor extends AbstractSensorModule<SOSVirtualSensorConfig> implements ISOSDataConsumer
{
    ISensorDescriptionStorage<?> smlStorage;
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
    public void updateSensor(AbstractProcess newSensorDescription) throws Exception
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
            obsOutputs.put(templateID, newOutput);
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
        for (DataBlock dataBlock: dataBlocks)
            ((SOSVirtualSensorOutput)obsOutputs.get(templateID)).publishNewRecord(dataBlock);
    }


    @Override
    public void start() throws SensorHubException
    {
        smlStorage = SensorHub.getInstance().getPersistenceManager().getSensorDescriptionStorage();
        
        // generate output interfaces from description
        for (AbstractSWEIdentifiable output: getCurrentSensorDescription().getOutputList())
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
            else
            {
                dataStruct = (DataComponent)output;
                dataEnc = BinaryEncodingImpl.getDefaultEncoding(dataStruct);
            }
            
            newResultTemplate(dataStruct, dataEnc);
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
    public boolean isSensorDescriptionHistorySupported()
    {
        return true;
    }


    @Override
    public AbstractProcess getCurrentSensorDescription() throws SensorException
    {
        return smlStorage.getSensorDescription(config.sensorUID);
    }


    @Override
    public AbstractProcess getSensorDescription(DateTime t) throws SensorException
    {
        return smlStorage.getSensorDescriptionAtTime(config.sensorUID, t.getTime());
    }


    @Override
    public void updateSensorDescription(AbstractProcess systemDesc, boolean recordHistory) throws SensorException
    {
        if (!recordHistory)
            smlStorage.removeHistory(systemDesc.getUniqueIdentifier());
        
        smlStorage.update(systemDesc);
    }


    @Override
    public boolean isConnected()
    {
        return true;
    }

}
