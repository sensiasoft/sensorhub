/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor.fakegps;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.Quantity;
import net.opengis.swe.v20.Time;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.data.DataRecordImpl;
import org.vast.data.QuantityImpl;
import org.vast.data.TextEncodingImpl;
import org.vast.data.TimeImpl;
import org.vast.swe.SWEConstants;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;


public class FakeGpsOutput extends AbstractSensorOutput<FakeGpsSensor>
{
    private static final Logger log = LoggerFactory.getLogger(FakeGpsOutput.class);
    DataComponent posDataStruct;
    DataEncoding posDataEncoding;
    List<double[]> trajPoints;
    boolean sendData;
    Timer timer;
    double currentTrackPos;
    

    public FakeGpsOutput(FakeGpsSensor parentSensor)
    {
        super(parentSensor);
        trajPoints = new ArrayList<double[]>();
    }


    @Override
    public String getName()
    {
        return "gpsLocation";
    }


    @Override
    protected void init()
    {
        // SWE Common data structure
        posDataStruct = new DataRecordImpl(3);
        posDataStruct.setName(getName());
        posDataStruct.setDefinition("http://sensorml.com/ont/swe/property/Location");
        
        Time c1 = new TimeImpl();
        c1.getUom().setHref(Time.ISO_TIME_UNIT);
        c1.setDefinition(SWEConstants.DEF_SAMPLING_TIME);
        posDataStruct.addComponent("time", c1);

        Quantity c;
        c = new QuantityImpl();
        c.getUom().setCode("deg");
        c.setDefinition("http://sensorml.com/ont/swe/property/Latitude");
        c.setReferenceFrame("http://www.opengis.net/def/crs/EPSG/0/4979");
        c.setAxisID("Lat");
        posDataStruct.addComponent("lat",c);

        c = new QuantityImpl();
        c.getUom().setCode("deg");
        c.setDefinition("http://sensorml.com/ont/swe/property/Longitude");
        c.setReferenceFrame("http://www.opengis.net/def/crs/EPSG/0/4979");
        c.setAxisID("Long");
        posDataStruct.addComponent("lon", c);

        c = new QuantityImpl();
        c.getUom().setCode("m");
        c.setDefinition("http://sensorml.com/ont/swe/property/Altitude");
        c.setReferenceFrame("http://www.opengis.net/def/crs/EPSG/0/4979");
        c.setAxisID("h");
        posDataStruct.addComponent("alt", c);
        
        posDataEncoding = new TextEncodingImpl(",", "\n");
    }


    private boolean generateRandomTrajectory()
    {
        FakeGpsConfig config = getParentModule().getConfiguration();
        
        // get or randomly generate start/end coordinates
        double startLat;
        double startLong;
        double endLat;
        double endLong;
        
        // meb: added ability to specify start and stop location
        if (trajPoints.isEmpty())
        {
        	
        	if (Double.isNaN(config.startLatitude)){
        		// if startLat not given, pick random start and stop within given area    		
	            startLat = config.centerLatitude + (Math.random()-0.5) * config.areaSize;
	            startLong = config.centerLongitude + (Math.random()-0.5) * config.areaSize;
	            endLat = config.centerLatitude + (Math.random()-0.5) * config.areaSize;
	            endLong = config.centerLongitude + (Math.random()-0.5) * config.areaSize;

        	}
        	else {
	        	// use start location provided in configuration
	            startLat = config.startLatitude;
	        	startLong = config.startLongitude;
	        	endLat = config.stopLatitude;
	        	endLong = config.stopLongitude;
        	}
        }
        else
        {
            // restart from end of previous track and go to random end
            double[] lastPoint = trajPoints.get(trajPoints.size()-1);
            startLat = lastPoint[0];
            startLong = lastPoint[1];
            endLat = config.centerLatitude + (Math.random()-0.5) * config.areaSize;
            endLong = config.centerLongitude + (Math.random()-0.5) * config.areaSize;
        } 
        
        
        try
        {
            // request directions using Google API
            URL dirRequest = new URL(config.googleApiUrl + "?origin=" + startLat + "," + startLong +
                    "&destination=" + endLat + "," + endLong);
            log.debug("Google API request: " + dirRequest);
            InputStream is = new BufferedInputStream(dirRequest.openStream());
            
            // parse JSON track
            JsonParser reader = new JsonParser();
            JsonElement root = reader.parse(new InputStreamReader(is));
            //System.out.println(root);
            JsonElement polyline = root.getAsJsonObject().get("routes").getAsJsonArray().get(0).getAsJsonObject().get("overview_polyline");
            String encodedData = polyline.getAsJsonObject().get("points").getAsString();
            
            // decode polyline data
            decodePoly(encodedData);
            currentTrackPos = 0.0;
            return true;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            trajPoints.clear();
            try { Thread.sleep(60000L); }
            catch (InterruptedException e1) {}
            return false;
        }
    }


    private void decodePoly(String encoded)
    {
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;        
        trajPoints.clear();
        
        while (index < len)
        {
            int b, shift = 0, result = 0;
            do
            {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            }
            while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do
            {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            }
            while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            double[] p = new double[] {(double) lat / 1E5, (double) lng / 1E5};
            trajPoints.add(p);
        }
    }
    
    
    private void sendMeasurement()
    {
        if (trajPoints.isEmpty() || currentTrackPos >= trajPoints.size()-2)
        {
            if (!generateRandomTrajectory())
                return;
            
            // skip if generated traj is too small
            if (trajPoints.size() < 2)
            {
                trajPoints.clear();
                return;
            }
            //for (double[] p: trajPoints)
            //     System.out.println(Arrays.toString(p));
        }
        
        // convert speed from km/h to lat/lon deg/s
        double speed = getParentModule().getConfiguration().vehicleSpeed / 20000 * 180 / 3600;
        int trackIndex = (int)currentTrackPos;
        double ratio = currentTrackPos - trackIndex;
        double[] p0 = trajPoints.get(trackIndex);
        double[] p1 = trajPoints.get(trackIndex+1);
        double dLat = p1[0] - p0[0];
        double dLon = p1[1] - p0[1];
        double dist = Math.sqrt(dLat*dLat + dLon*dLon);        
        
        // compute new position
        double time = System.currentTimeMillis() / 1000.;
        double lat = p0[0] + dLat*ratio;
        double lon = p0[1] + dLon*ratio;
        double alt = 193;
        
        // build and publish datablock
        DataBlock dataBlock = posDataStruct.createDataBlock();
        dataBlock.setDoubleValue(0, time);
        dataBlock.setDoubleValue(1, lat);
        dataBlock.setDoubleValue(2, lon);
        dataBlock.setDoubleValue(3, alt);
        
        // update latest record and send event
        latestRecord = dataBlock;
        latestRecordTime = System.currentTimeMillis();
        eventHandler.publishEvent(new SensorDataEvent(latestRecordTime, FakeGpsOutput.this, dataBlock));
        
        currentTrackPos += speed / dist;
    }


    protected void start()
    {
        if (timer != null)
            return;
        timer = new Timer();
        
        // start main measurement generation thread
        TimerTask task = new TimerTask() {
            public void run()
            {
                sendMeasurement();
            }            
        };
        
        timer.scheduleAtFixedRate(task, 0, (long)(getAverageSamplingPeriod()*1000));        
    }


    protected void stop()
    {
        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return 1.0;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return posDataStruct;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return posDataEncoding;
    }

}
