/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import net.opengis.OgcProperty;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.TimeIndeterminateValue;
import net.opengis.gml.v32.TimePosition;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractPhysicalProcess;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.PositionConfig.LLALocation;
import org.sensorhub.api.sensor.PositionConfig.EulerOrientation;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.utils.MsgUtils;
import org.vast.ogc.om.SamplingPoint;
import org.vast.sensorML.PhysicalSystemImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Class providing default implementation of common sensor API methods.<br/>
 * This can be used as the base for most sensor driver implementations as it
 * generates defaults for the following:
 * <ul>
 * <li>A random Unique ID using a UUID (the same is used between restarts)</li>
 * <li>A short XML ID</li>
 * <li>A default SensorML description including IDs, temporal validity, I/Os
 * and position (location + orientation) if the sensor configuration provides
 * static location and/or orientation</li>
 * <li>A feature of interest if the sensor configuration provides static
 * location</li>
 * </ul>
 * </p>
 * <p>
 * All of these items can be overriden by derived classes.<br/>
 * It also provides helper methods to implement automatic reconnection.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ConfigType> 
 * @since Oct 30, 2014
 */
public abstract class AbstractSensorModule<ConfigType extends SensorConfig> extends AbstractModule<ConfigType> implements ISensorModule<ConfigType>
{
    public final static String DEFAULT_ID = "SENSOR_";
    protected final static String LOCATION_OUTPUT_ID = "SENSOR_LOCATION";
    protected final static String LOCATION_OUTPUT_NAME = "sensorLocation";
    protected final static String ERROR_NO_UPDATE = "Sensor Description update is not supported by driver ";
    protected final static String ERROR_NO_HISTORY = "History of sensor description is not supported by driver ";
    protected final static String ERROR_NO_ENTITIES = "Multiple entities are not supported by driver ";
    
    protected final static String UUID_URI_PREFIX = "urn:uuid:";
    protected final static String STATE_UNIQUE_ID = "UniqueID";
    protected final static String STATE_LAST_SML_UPDATE = "LastUpdatedSensorDescription";
    
    private Map<String, ISensorDataInterface> obsOutputs = new LinkedHashMap<String, ISensorDataInterface>();
    private Map<String, ISensorDataInterface> statusOutputs = new LinkedHashMap<String, ISensorDataInterface>();
    private Map<String, ISensorControlInterface> controlInputs = new LinkedHashMap<String, ISensorControlInterface>();
        
    protected DefaultLocationOutput<?> locationOutput;
    protected AbstractPhysicalProcess sensorDescription = new PhysicalSystemImpl();
    protected long lastUpdatedSensorDescription = Long.MIN_VALUE;
    
    protected String xmlID;
    protected String uniqueID;
    protected boolean saveUniqueID;
    protected AbstractFeature foi = null;
            
    
    @Override
    public void init() throws SensorHubException
    {
        // reset internal state
        this.uniqueID = null;
        this.xmlID = null;
        this.foi = null;
        this.locationOutput = null;
        this.sensorDescription = new PhysicalSystemImpl();
        removeAllOutputs();
        removeAllControlInputs();        
    }
    
    
    @Override
    public synchronized void init(ConfigType config) throws SensorHubException
    {
        super.init(config);
        
        if (this.uniqueID == null)
        {
            String uuid = UUID.randomUUID().toString();
            this.uniqueID = UUID_URI_PREFIX + uuid;
        
            if (this.xmlID == null)
            {
                int endIndex = Math.min(8, uuid.length());
                String shortId = uuid.substring(0, endIndex);
                this.xmlID = DEFAULT_ID + shortId;
            }
        }
    }


    /**
     * Call this method to add each sensor observation or status output
     * @param dataInterface interface to add as sensor output
     * @param isStatus set to true when registering a status output
     */
    protected void addOutput(ISensorDataInterface dataInterface, boolean isStatus)
    {
        if (isStatus)
            statusOutputs.put(dataInterface.getName(), dataInterface);
        else
            obsOutputs.put(dataInterface.getName(), dataInterface);
    }
    
    
    /**
     * Helper method to add a location output so that all sensors can update their location
     * in a consistent manner.
     * @param updatePeriod estimated location update period or NaN if sensor is mostly static
     */
    protected void addLocationOutput(double updatePeriod)
    {
        // TODO deal with other CRS than 4979
        locationOutput = new DefaultLocationOutputLLA<AbstractSensorModule<?>>(this, updatePeriod);
        addOutput(locationOutput, true);
    }
    
    
    /**
     * Removes all outputs previously added to this sensor
     */
    protected void removeAllOutputs()
    {
        statusOutputs.clear();
        obsOutputs.clear();
    }
    
    
    /**
     * Call this method to add each sensor control input
     * @param controlInterface interface to add as sensor control input
     */
    protected void addControlInput(ISensorControlInterface controlInterface)
    {
        controlInputs.put(controlInterface.getName(), controlInterface);
    }
    
    
    /**
     * Removes all control inputs previously added to this sensor
     */
    protected void removeAllControlInputs()
    {
        controlInputs.clear();
    }
    
    
    @Override
    public String getUniqueIdentifier()
    {
        return uniqueID;
    }
    

    @Override
    public AbstractPhysicalProcess getCurrentDescription()
    {
        synchronized (sensorDescription)
        {
            if (sensorDescription == null || !sensorDescription.isSetIdentifier())
                updateSensorDescription();
        
            return sensorDescription;
        }
    }
    
    
    @Override
    public long getLastDescriptionUpdate()
    {
        return lastUpdatedSensorDescription;
    }
    
    
    /**
     * This method should be called whenever the sensor description needs to be regenerated.<br/>
     * This default implementation reads the base description from the SensorML file if provided
     * and then appends the unique sensor identifier, time validity and the description of all
     * registered outputs and control inputs. This will also update the lastUpdatedSensorDescription
     * time stamp and send a SENSOR_CHANGED event when 
     * @throws SensorException
     */
    protected void updateSensorDescription()
    {
        synchronized (sensorDescription)
        {
            // by default we return the static description provided in config
            String smlUrl = config.getSensorDescriptionURL();
            if (smlUrl != null && smlUrl.length() > 0)
            {
                try
                {
                    SMLUtils utils = new SMLUtils(SMLUtils.V2_0);
                    InputStream is = new URL(smlUrl).openStream();
                    sensorDescription = (AbstractPhysicalProcess)utils.readProcess(is);
                }
                catch (IOException e)
                {
                    throw new IllegalStateException("Error while parsing static SensorML description for sensor " +
                                                    MsgUtils.moduleString(this), e);
                }
            }
            else
            {
                sensorDescription = new PhysicalSystemImpl();
            }
            
            //////////////////////////////////////////////////////////////
            // add stuffs if not already defined in static SensorML doc //
            //////////////////////////////////////////////////////////////
            if (lastUpdatedSensorDescription == Long.MIN_VALUE)
                lastUpdatedSensorDescription = System.currentTimeMillis();
            double newValidityTime = lastUpdatedSensorDescription / 1000.;
            
            // default IDs
            String gmlId = sensorDescription.getId();
            if (gmlId == null || gmlId.length() == 0)
                sensorDescription.setId(xmlID);
            if (!sensorDescription.isSetIdentifier())
                sensorDescription.setUniqueIdentifier(uniqueID);
            
            // description
            if (sensorDescription.getName() == null && config.name != null)
                sensorDescription.setName(config.name);
            
            // time validity
            if (sensorDescription.getNumValidTimes() == 0)
            {
                GMLFactory fac = new GMLFactory();
                TimePosition begin = fac.newTimePosition(newValidityTime);
                TimePosition end = fac.newTimePosition();
                end.setIndeterminatePosition(TimeIndeterminateValue.NOW);
                sensorDescription.addValidTimeAsTimePeriod(fac.newTimePeriod(begin, end));
            }
            
            // outputs
            if (sensorDescription.getNumOutputs() == 0)
            {
                for (Entry<String, ? extends ISensorDataInterface> output: getAllOutputs().entrySet())
                {
                    DataComponent outputDesc = output.getValue().getRecordDescription();
                    if (outputDesc == null)
                        continue;
                    outputDesc = outputDesc.copy();
                    sensorDescription.addOutput(output.getKey(), outputDesc);
                }
            }
            
            // control parameters
            if (sensorDescription.getNumParameters() == 0)
            {
                for (Entry<String, ? extends ISensorControlInterface> param: getCommandInputs().entrySet())
                {
                    DataComponent paramDesc = param.getValue().getCommandDescription();
                    if (paramDesc == null)
                        continue;
                    paramDesc = paramDesc.copy();
                    paramDesc.setUpdatable(true);
                    sensorDescription.addParameter(param.getKey(), paramDesc);
                }
            }
            
            // sensor position
            String localFrameRef = '#' + getLocalFrameID();
            GeoPosHelper fac = new GeoPosHelper();
            Vector locVector = null;
            Vector orientVector = null;
            
            // get static location from config if available
            LLALocation loc = config.getLocation();
            if (loc != null)
            {
                locVector = fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
                locVector.assignNewDataBlock();
                locVector.getComponent(0).getData().setDoubleValue(loc.lat);
                locVector.getComponent(1).getData().setDoubleValue(loc.lon);
                locVector.getComponent(2).getData().setDoubleValue(loc.alt);
                locVector.setLocalFrame(localFrameRef);
            }
            
            // get static orientation from config if available
            EulerOrientation orient = config.getOrientation();
            if (orient != null)
            {
                orientVector = fac.newEulerOrientationENU(SWEConstants.DEF_SENSOR_ORIENT);
                orientVector.assignNewDataBlock();
                orientVector.getComponent(0).getData().setDoubleValue(orient.heading);
                orientVector.getComponent(1).getData().setDoubleValue(orient.pitch);
                orientVector.getComponent(2).getData().setDoubleValue(orient.roll);
                orientVector.setLocalFrame(localFrameRef);
            }
            
            if (locVector != null || orientVector != null)
            {
                if (orientVector == null) // only location
                    sensorDescription.addPositionAsVector(locVector);
                else if (locVector == null) // only orientation
                    sensorDescription.addPositionAsVector(orientVector);
                else // both
                {
                    DataRecord pos = fac.newDataRecord(2);
                    pos.addField("location", locVector);
                    pos.addField("orientation", orientVector);
                    sensorDescription.addPositionAsDataRecord(pos);
                }
            }
            
            // else reference location output if any
            else if (locationOutput != null)
            {
                // set ID of reference frame on location output                    
                ((Vector)locationOutput.getRecordDescription()).setLocalFrame(localFrameRef);
                
                // if update rate is high, set sensorML location as link to output
                if (locationOutput.getAverageSamplingPeriod() < 3600.)
                {
                    locationOutput.getRecordDescription().setId(LOCATION_OUTPUT_ID);
                    OgcProperty<?> linkProp = SWEHelper.newLinkProperty("#" + LOCATION_OUTPUT_ID);
                    ((AbstractPhysicalProcess)sensorDescription).getPositionList().add(linkProp);
                }
            }
        }
    }
    
    
    protected void notifyNewDescription(long updateTime)
    {
        // send event
        lastUpdatedSensorDescription = updateTime;
        eventHandler.publishEvent(new SensorEvent(updateTime, this, SensorEvent.Type.SENSOR_CHANGED));
    }
    
    
    /**
     * Updates the sensor location. If a sensor location output is present, the new location
     * will be send through this interface. The location provided in the SensorML description
     * will also be updated unless it references the location output. 
     * @param x
     * @param y
     * @param z
     */
    protected void updateLocation(double time, double x, double y, double z)
    {
        // send new location through output
        if (locationOutput != null)
            locationOutput.updateLocation(time, x, y, z);
        
        // update sensorML description
        AbstractProcess processDesc = getCurrentDescription();
        if (processDesc instanceof AbstractPhysicalProcess)
        {
            AbstractPhysicalProcess sensorDesc = (AbstractPhysicalProcess)processDesc;
            
            // update GML location if point template was provided
            AbstractGeometry gmlLoc = sensorDesc.getLocation();
            if (gmlLoc != null && gmlLoc instanceof Point)
            {
                double[] pos;
                
                if (Double.isNaN(z))
                {
                    gmlLoc.setSrsName(SWEHelper.getEpsgUri(4326));
                    pos = new double[2];
                }
                else
                {
                    gmlLoc.setSrsName(SWEHelper.getEpsgUri(4979));
                    pos = new double[3];
                    pos[2] = z;
                }
                                
                pos[0] = y;
                pos[1] = x;
                ((Point)gmlLoc).setPos(pos);
            }
            
            // else update SWE structure for sensor position
            else
            {
                // update location vector
                GeoPosHelper fac = new GeoPosHelper();
                Vector locVector = fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
                locVector.setLocalFrame('#' + getLocalFrameID());
                updateSensorPosition(sensorDesc, locVector, null);
            }
            
            // send sensorML changed event
            long now = System.currentTimeMillis();
            notifyNewDescription(now);
        }
    }
    
    
    protected String getLocalFrameID()
    {
        return "REF_FRAME_" + xmlID;
    }
    
    
    /**
     * Updates the sensor location and orientation in the SensorML document
     * @param sensorDesc
     * @param newLoc new location vector or null if location shouldn't be updated
     * @param newOrient new orientation vector or null if location shouldn't be updated
     */
    protected void updateSensorPosition(AbstractPhysicalProcess sensorDesc, Vector newLoc, Vector newOrient)
    {
        String localFrameRef = '#' + getLocalFrameID();
        Iterator<Object> it = sensorDesc.getPositionList().iterator();
        int i = 0;
        while (it.hasNext())
        {
            Object obj = it.next();
            
            if (obj instanceof Vector)
            {
                if (newLoc != null && ((Vector) obj).getLocalFrame().equals(localFrameRef))
                {
                    sensorDesc.getPositionList().set(i, newLoc);
                    break;
                }
            }
            
            else if (obj instanceof DataRecord)
            {
                // TODO add both location and orientation
            }
            
            i++;
        }
    }


    @Override
    public Map<String, ISensorDataInterface> getAllOutputs()
    {
        Map<String, ISensorDataInterface> allOutputs = new LinkedHashMap<String, ISensorDataInterface>();  
        allOutputs.putAll(obsOutputs);
        allOutputs.putAll(statusOutputs);
        return Collections.unmodifiableMap(allOutputs);
    }


    @Override
    public Map<String, ISensorDataInterface> getStatusOutputs()
    {
        return Collections.unmodifiableMap(statusOutputs);
    }


    @Override
    public Map<String, ISensorDataInterface> getObservationOutputs()
    {
        return Collections.unmodifiableMap(obsOutputs);
    }


    @Override
    public Map<String, ISensorControlInterface> getCommandInputs()
    {
        return Collections.unmodifiableMap(controlInputs);
    }
    
    
    @Override
    public AbstractFeature getCurrentFeatureOfInterest()
    {
        // add default feature of interest if location is set in config
        LLALocation loc = config.getLocation();
        if (foi == null && loc != null)
        {            
            SamplingPoint sf = new SamplingPoint();
            sf.setId(xmlID + "_FOI");
            sf.setUniqueIdentifier(uniqueID + "-foi");
            if (config.name != null)
                sf.setName(config.name);
            sf.setDescription("Sampling point for " + config.name);
            sf.setHostedProcedureUID(uniqueID);
            Point point = new GMLFactory().newPoint();
            point.setSrsName(SWEConstants.REF_FRAME_4979);
            point.setSrsDimension(3);
            point.setPos(new double[] {loc.lat, loc.lon, loc.alt});
            sf.setShape(point);
            this.foi = sf;
        }
        
        return foi;
    }


    @Override
    public void updateConfig(ConfigType config) throws SensorHubException
    {
        super.updateConfig(config);
        if (config.sensorML != null)
        {
            // TODO detect if SensorML has really changed
            updateSensorDescription();
            notifyNewDescription(System.currentTimeMillis());
        }
    }

    
    @Override
    public void loadState(IModuleStateManager loader) throws SensorHubException
    {
        super.loadState(loader);
        
        // set unique ID to the one previously saved
        String uniqueID = loader.getAsString(STATE_UNIQUE_ID);
        if (uniqueID != null && this.uniqueID.startsWith(UUID_URI_PREFIX))
            this.uniqueID = uniqueID;
        
        Long lastUpdateTime = loader.getAsLong(STATE_LAST_SML_UPDATE);
        if (lastUpdateTime != null)
            this.lastUpdatedSensorDescription = lastUpdateTime;
    }
    
    
    @Override
    public void saveState(IModuleStateManager saver) throws SensorHubException
    {
        super.saveState(saver);
        
        // save unique ID if it was automatically generated as UUID
        if (uniqueID != null && uniqueID.startsWith(UUID_URI_PREFIX))
            saver.put(STATE_UNIQUE_ID, this.uniqueID);
        
        saver.put(STATE_LAST_SML_UPDATE, this.lastUpdatedSensorDescription);
        saver.flush();
    }
}
