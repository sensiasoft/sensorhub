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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.opengis.OgcProperty;
import net.opengis.OgcPropertyList;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.AbstractGeometry;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.TimeIndeterminateValue;
import net.opengis.gml.v32.TimePosition;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractPhysicalProcess;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.SpatialFrame;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Vector;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateManager;
import org.sensorhub.api.sensor.ISensorControlInterface;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.utils.MsgUtils;
import org.vast.sensorML.PhysicalSystemImpl;
import org.vast.sensorML.SMLUtils;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.swe.helper.GeoPosHelper;


/**
 * <p>
 * Class providing default implementation of common sensor API methods.
 * This can be used as the base for most sensor driver implementations.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ConfigType> 
 * @since Oct 30, 2014
 */
public abstract class AbstractSensorModule<ConfigType extends SensorConfig> extends AbstractModule<ConfigType> implements ISensorModule<ConfigType>
{
    public final static String DEFAULT_ID = "SENSOR_DESC";
    protected final static String LOCATION_OUTPUT_ID = "SENSOR_LOCATION";
    protected final static String LOCATION_OUTPUT_NAME = "sensorLocation";
    protected final static String ERROR_NO_UPDATE = "Sensor Description update is not supported by driver ";
    protected final static String ERROR_NO_HISTORY = "History of sensor description is not supported by driver ";
    protected final static String ERROR_NO_ENTITIES = "Multiple entities are not supported by driver ";
    
    protected final static String STATE_UNIQUE_ID = "UniqueID";
    protected final static String STATE_LAST_SML_UPDATE = "LastUpdatedSensorDescription";
    
    private Map<String, ISensorDataInterface> obsOutputs = new LinkedHashMap<String, ISensorDataInterface>();
    private Map<String, ISensorDataInterface> statusOutputs = new LinkedHashMap<String, ISensorDataInterface>();
    private Map<String, ISensorControlInterface> controlInputs = new LinkedHashMap<String, ISensorControlInterface>();
    private volatile boolean wasConnected = false;
    
    protected DefaultLocationOutput<?> locationOutput;
    protected AbstractProcess sensorDescription = new PhysicalSystemImpl();
    protected long lastUpdatedSensorDescription = Long.MIN_VALUE;
    protected String uniqueID;
            
    
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
        // to stay backward compatible
        if (uniqueID == null)
            return this.getCurrentDescription().getUniqueIdentifier();
        
        return uniqueID;
    }
    

    @Override
    public AbstractProcess getCurrentDescription()
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
                sensorDescription.setId(DEFAULT_ID);
            if (!sensorDescription.isSetIdentifier())
                sensorDescription.setUniqueIdentifier(getLocalID());
            
            // description
            if (!sensorDescription.isSetDescription() && config.name != null)
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
            
            // position
            if (locationOutput != null && sensorDescription instanceof AbstractPhysicalProcess)
            {
                // set ID of reference frame on location output
                String localFrame = getLocalFrameID((AbstractPhysicalProcess)sensorDescription);
                ((Vector)locationOutput.getRecordDescription()).setLocalFrame(localFrame);
                
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
            
            // update position
            OgcPropertyList<Object> posList = sensorDesc.getPositionList();
            if (!posList.isEmpty())
            {
                /*for (Object posObj: posList)
                {
                    
                }*/
            }
            else
            {
                GeoPosHelper fac = new GeoPosHelper();
                Vector locVector = fac.newLocationVectorLLA(SWEConstants.DEF_SENSOR_LOC);
                locVector.setLocalFrame(getLocalFrameID(sensorDesc));
                sensorDesc.addPositionAsVector(locVector);
            }
            
            // send sensorML changed event
            long now = System.currentTimeMillis();
            notifyNewDescription(now);
        }
    }
    
    
    protected String getLocalFrameID(AbstractPhysicalProcess sensorDesc)
    {
        List<SpatialFrame> refFrames = sensorDesc.getLocalReferenceFrameList();
        if (refFrames == null || refFrames.isEmpty())
            return null;
        
        return refFrames.get(0).getId();
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
        return null;
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
        
        this.uniqueID = loader.getAsString(STATE_UNIQUE_ID);
        
        Long lastUpdateTime = loader.getAsLong(STATE_LAST_SML_UPDATE);
        if (lastUpdateTime != null)
            this.lastUpdatedSensorDescription = lastUpdateTime;
    }
    
    
    @Override
    public void saveState(IModuleStateManager saver) throws SensorHubException
    {
        super.saveState(saver);
        
        if (uniqueID != null)
            saver.put(STATE_UNIQUE_ID, this.uniqueID);
        
        saver.put(STATE_LAST_SML_UPDATE, this.lastUpdatedSensorDescription);
        saver.flush();
    }
    
    
    /**
     * Helper method to wait until the sensor is connected or timeout occurs.<br/>
     * If connection is detected by another thread, this method can also be notified
     * by calling stateLock.notify()
     * @param retryPeriod retry period in milliseconds
     * @param timeOut timeout period in milliseconds
     * @return true if sensor was connected, false if timeout was reached
     */
    protected boolean waitForConnection(long retryPeriod, long timeOut) throws SensorException
    {
        long beginTime = System.currentTimeMillis();
        long lastCheckTime = beginTime;
        
        synchronized (stateLock)
        {
            try
            {
                while (!isConnected())
                {
                    long now = System.currentTimeMillis();
                    if (now - beginTime > timeOut)
                        throw new SensorException("Sensor connection timeout");
                    
                    long sleepPeriod = retryPeriod - (now - lastCheckTime);
                    if (sleepPeriod > 0)
                    {
                        getLogger().debug("Cannot connect to sensor. Retrying in {} ms", sleepPeriod);
                        stateLock.wait(sleepPeriod);
                    }
                    else
                        getLogger().debug("Cannot connect to sensor. Retrying now");
                    
                    lastCheckTime = System.currentTimeMillis();
                }

                notifyConnectionStatus(true);
            }
            catch (InterruptedException e)
            {
            }
        }
        
        return true;
    }
    
    
    /**
     * Helper method to send and log connection/disconnection events
     * @param connected
     */
    protected void notifyConnectionStatus(boolean connected)
    {
        long now = System.currentTimeMillis();
        
        // only log and send event if status has actually changed
        if (connected != wasConnected)
        {
            if (connected)
            {
                getLogger().info("Sensor is connected");
                eventHandler.publishEvent(new SensorEvent(now, this, SensorEvent.Type.CONNECTED));
            }
            else
            {
                getLogger().info("Sensor is disconnected");
                eventHandler.publishEvent(new SensorEvent(now, this, SensorEvent.Type.DISCONNECTED));
            }
            
            wasConnected = connected;
        }
    }
    
    
    /**
     * Helper method to restart the driver after a disconnection
     */
    protected void restartOnDisconnect()
    {
        notifyConnectionStatus(false);
        
        // restart in separate thread
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    stop();
                    start();
                }
                catch (SensorHubException e)
                {
                    getLogger().error("Error while reconnecting to sensor");
                }
            }
        }).start();
    }
}
