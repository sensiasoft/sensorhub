package org.sensorhub.impl.sensor.station.metar;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.impl.sensor.AbstractSensorModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.sensorML.SMLHelper;


public class MetarSensor extends AbstractSensorModule<MetarConfig> implements IMultiSourceDataProducer //extends StationSensor
{
    static final Logger log = LoggerFactory.getLogger(MetarSensor.class);
    static final String SENSOR_UID_PREFIX = "urn:test:sensors:weather:metar:";
    static final String STATION_UID_PREFIX = SENSOR_UID_PREFIX + "station";
    
    Set<String> foiIDs;
    Map<String, PhysicalSystem> stationFois;
    Map<String, PhysicalSystem> stationDesc;
    MetarOutput metarInterface;
    
	
	public MetarSensor()
	{
	    this.foiIDs = new LinkedHashSet<String>();
	    this.stationFois = new LinkedHashMap<String, PhysicalSystem>();
	    this.stationDesc = new LinkedHashMap<String, PhysicalSystem>();
	}
	
	
	@Override
	public void init(MetarConfig config) throws SensorHubException
    {
        super.init(config);
        
        this.metarInterface = new MetarOutput(this);        
        addOutput(metarInterface, false);
        metarInterface.init();        
    }

	
	@Override
	protected void updateSensorDescription()
	{
		synchronized (sensorDescription)
		{
			super.updateSensorDescription();
			sensorDescription.setUniqueIdentifier(SENSOR_UID_PREFIX + "network");
			sensorDescription.setDescription("METAR weather station network");
			
			// append href to all stations composing the network
			for (String stationID: config.stationIDs)
			{
			    String name = "station" + stationID;
			    String href = STATION_UID_PREFIX + stationID;
			    ((PhysicalSystem)sensorDescription).getComponentList().add(name, href, null);
			}
		}
	}


	@Override
	public void start() throws SensorHubException
	{
	    SMLHelper smlFac = new SMLHelper();
	    GMLFactory gmlFac = new GMLFactory(true);
	    
	    // generate station FOIs and full descriptions
	    for (String stationID: config.stationIDs)
        {
	        // TODO fetch station info (name, location, etc.)
	        
	        String uid = STATION_UID_PREFIX + stationID;
	        String name = "K***";
	        String description = "METAR weather station #" + stationID;
	        
	        // generate small SensorML for FOI (in this case the system is the FOI)
	        PhysicalSystem foi = smlFac.newPhysicalSystem();
	        foi.setId(stationID);
	        foi.setUniqueIdentifier(uid);
	        foi.setName(name);
	        foi.setDescription(description);
	        Point stationLoc = gmlFac.newPoint();
	        double coord = Double.parseDouble(stationID) / 100.0;
	        stationLoc.setPos(new double[] {coord, coord/2.0, 0.0});
	        foi.setLocation(stationLoc);
	        stationFois.put(uid, foi);
	        foiIDs.add(uid);
	        
	        // TODO generate full SensorML for sensor description
	        PhysicalSystem sensorDesc = smlFac.newPhysicalSystem();
	        sensorDesc.setId("STATION_" + stationID);
	        sensorDesc.setUniqueIdentifier(uid);
            sensorDesc.setName(name);
            sensorDesc.setDescription(description);
            stationDesc.put(uid, sensorDesc);
        }
	    
	    metarInterface.start();        
	}


	@Override
	public void stop() throws SensorHubException
	{
		metarInterface.stop();
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


    @Override
    public Collection<String> getEntityIDs()
    {
        return Collections.unmodifiableCollection(stationFois.keySet());
    }
    
    
    @Override
    public AbstractFeature getCurrentFeatureOfInterest()
    {
        return null;  // or throw unsupported exception/ or aggregate of all FOI
    }


    @Override
    public AbstractProcess getCurrentDescription(String entityID)
    {
        return stationDesc.get(entityID);
    }


    @Override
    public double getLastDescriptionUpdate(String entityID)
    {
        return 0;
    }


    @Override
    public AbstractFeature getCurrentFeatureOfInterest(String entityID)
    {
        return stationFois.get(entityID);
    }


    @Override
    public Collection<? extends AbstractFeature> getFeaturesOfInterest()
    {
        return Collections.unmodifiableCollection(stationFois.values());
    }
    
    
    @Override
    public Collection<String> getFeaturesOfInterestIDs()
    {
        return Collections.unmodifiableCollection(foiIDs);
    }
}
