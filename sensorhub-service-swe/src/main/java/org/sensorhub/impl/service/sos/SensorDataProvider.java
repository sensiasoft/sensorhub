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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorDataEvent;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.api.service.ServiceException;
import org.sensorhub.utils.MsgUtils;
import org.vast.data.DataIterator;
import org.vast.ogc.def.DefinitionRef;
import org.vast.ogc.gml.FeatureRef;
import org.vast.ogc.om.IObservation;
import org.vast.ogc.om.ObservationImpl;
import org.vast.ogc.om.ProcedureRef;
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.swe.SWEConstants;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Implementation of SOS data provider connecting to a sensor via 
 * SensorHub's sensor API (ISensorDataInterface)
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class SensorDataProvider implements ISOSDataProvider, IEventListener
{
    ISensorModule<?> sensor;
    List<ISensorDataInterface> dataSources;
    BlockingDeque<SensorDataEvent> eventQueue;
    List<Timer> pollTimers;
    long timeOut;
    long stopTime;
    
    SensorDataEvent lastDataEvent;
    int nextEventRecordIndex = 0;
            
    
    public SensorDataProvider(ISensorModule<?> srcSensor, SOSDataFilter filter) throws ServiceException
    {
        this.sensor = srcSensor;
        this.dataSources = new ArrayList<ISensorDataInterface>();
        this.eventQueue = new LinkedBlockingDeque<SensorDataEvent>(1);
        this.pollTimers = new ArrayList<Timer>();
        
        // figure out stop time (if any)
        stopTime = ((long)filter.getTimeRange().getStopTime()) * 1000L;
        
        // get list of desired sensor outputs
        try
        {
            // loop through all outputs and connect to the ones containing observables we need
            for (ISensorDataInterface outputInterface: sensor.getAllOutputs().values())
            {
                // skip if disabled
                if (!outputInterface.isEnabled())
                    continue;
                
                // keep it if we can find one of the observables
                DataIterator it = new DataIterator(outputInterface.getRecordDescription());
                while (it.hasNext())
                {
                    String defUri = (String)it.next().getDefinition();
                    if (filter.getObservables().contains(defUri))
                    {
                        // set to time out if no data is received after 10 sampling periods or min 5s
                        timeOut = (long)(outputInterface.getAverageSamplingPeriod() * 10. * 1000.);
                        timeOut = Math.max(timeOut, 5000L);
                        dataSources.add(outputInterface);
                        
                        // break for now since we support only requesting data from one output at a time
                        // TODO support case of multiple outputs since it is technically possible with GetObservation
                        break; 
                    }
                }
            }
        }
        catch (SensorException e)
        {
            throw new ServiceException("Error while fetching output description for sensor " + MsgUtils.moduleString(sensor), e);
        }
        
        // if everything went well listen or poll sensor outputs
        for (final ISensorDataInterface outputInterface: dataSources)
        {
            // case of time instant = now, just return latest record
            if (isNowTimeInstant(filter.getTimeRange()))
            {
                try
                {
                    double lastRecordTime = outputInterface.getLatestRecordTime();
                    DataBlock data = outputInterface.getLatestRecord();
                    eventQueue.offerLast(new SensorDataEvent(lastRecordTime, outputInterface, data));
                    stopTime = Long.MAX_VALUE; // make sure stoptime does not cause us to return null
                    timeOut = 0L;
                }
                catch (SensorException e)
                {
                   throw new ServiceException("Cannot get latest record from sensor " + sensor.getName(), e);
                }
            }
            
            // otherwise register listener if push is supported
            else if (outputInterface.isPushSupported())
                outputInterface.registerListener(this);
                        
            // otherwise setup timer task to poll regularly
            else
            {
                TimerTask pollTask = new TimerTask()
                {
                    double lastRecordTime = Double.NEGATIVE_INFINITY;
                    
                    @Override
                    public void run()
                    {
                        double time = outputInterface.getLatestRecordTime();
                        
                        // wait until a new record is available
                        if (!Double.isNaN(time) && time > lastRecordTime)
                        {
                            try
                            {
                                // add event to queue to simulate push case
                                DataBlock data = outputInterface.getLatestRecord();                            
                                eventQueue.offerLast(new SensorDataEvent(time, outputInterface, data));
                                lastRecordTime = time;
                            }
                            catch (SensorException e)
                            {
                                e.printStackTrace();
                            } 
                        }
                    }                
                };
                
                // poll at twice the sampling rate
                Timer timer = new Timer(sensor.getName() + " Polling", true);
                timer.scheduleAtFixedRate(pollTask, 0, (long)(outputInterface.getAverageSamplingPeriod() * 500.));
            }
        }
    }
    
    
    protected boolean isNowTimeInstant(TimeExtent timeFilter)
    {
        if (timeFilter.isTimeInstant() && timeFilter.isBaseAtNow())
            return true;
        
        return false;
    }
    
    
    @Override
    public IObservation getNextObservation() throws Exception
    {
        DataComponent result = getNextComponent();
        if (result == null)
            return null;
        
        // get phenomenon time from record 'SamplingTime' if present
        // otherwise use current time
        double samplingTime = System.currentTimeMillis()/1000.;
        for (int i=0; i<result.getComponentCount(); i++)
        {
            DataComponent comp = result.getComponent(i);
            if (comp.isSetDefinition())
            {
                String def = comp.getDefinition();
                if (def.equals(SWEConstants.DEF_SAMPLING_TIME))
                {
                    samplingTime = comp.getData().getDoubleValue();
                }
            }
        }
        
        TimeExtent phenTime = new TimeExtent();
        phenTime.setBaseTime(samplingTime);
        
        // use same value for resultTime for now
        TimeExtent resultTime = new TimeExtent();
        resultTime.setBaseTime(samplingTime);        
        
        // create observation object
        IObservation obs = new ObservationImpl();
        obs.setFeatureOfInterest(new FeatureRef("http://TODO"));
        obs.setObservedProperty(new DefinitionRef("http://TODO"));
        obs.setProcedure(new ProcedureRef(sensor.getCurrentSensorDescription().getUniqueIdentifier()));
        obs.setPhenomenonTime(phenTime);
        obs.setResultTime(resultTime);
        obs.setResult(result);
        
        return obs;
    }
    
    
    private DataComponent getNextComponent() throws SensorException
    {
        DataBlock data = getNextResultRecord();
        if (data == null)
            return null;
        
        DataComponent copyComponent = getResultStructure().copy();
        copyComponent.setData(data);
        return copyComponent;
    }
    

    @Override
    public DataBlock getNextResultRecord() throws SensorException
    {
        if (!hasMoreData())
            return null;
        
        try
        {
            // only poll next event from queue once we have returned all records associated to last event
            if (lastDataEvent == null || nextEventRecordIndex >= lastDataEvent.getRecords().length)
            {
                lastDataEvent = eventQueue.pollFirst(timeOut, TimeUnit.MILLISECONDS);
                if (lastDataEvent == null)
                    return null;
                
                // we stop if record is passed the given stop date
                if (lastDataEvent.getTimeStamp() > stopTime)
                    return null;
                
                nextEventRecordIndex = 0;
            }
            
            //System.out.println("->" + new DateTimeFormat().formatIso(lastDataEvent.getTimeStamp()/1000., 0));
            return lastDataEvent.getRecords()[nextEventRecordIndex++];
            
            // TODO add choice token value if request includes several outputs
        }
        catch (InterruptedException e)
        {
            return null;
        }
    }
    
    
    /*
     * For real-time streams, more data is always available unless
     * sensor is disabled or all sensor outputs are disabled
     */
    private boolean hasMoreData()
    {
        if (!sensor.isEnabled())
            return false;
        
        boolean interfaceActive = false;
        for (ISensorDataInterface source: dataSources)
        {
            if (source.isEnabled()) {
                interfaceActive = true;
                break;
            }
        }
        
        return interfaceActive;
    }
    

    @Override
    public DataComponent getResultStructure() throws SensorException
    {
        // TODO generate choice if request includes several outputs
        
        return dataSources.get(0).getRecordDescription();
    }
    

    @Override
    public DataEncoding getDefaultResultEncoding() throws Exception
    {
        return dataSources.get(0).getRecommendedEncoding();
    }
    
    
    @Override
    public void handleEvent(Event e)
    {
        if (e instanceof SensorEvent)
        {
            if (((SensorEvent) e).getType() == SensorEvent.Type.DISCONNECTED)
            {
                
            }
        }
        else if (e instanceof DataEvent)
        {
            if (((DataEvent) e).getType() == DataEvent.Type.NEW_DATA_AVAILABLE)
            {
                // TODO there is no guarantee that records are processed in chronological order
                // this is because events may not be received in chronological order in the 1st place
                // it's not as simple as using a sorting queue because we never know when is the next event!
                // we could use the average sampling period to decide how much to wait to confirm the order
                eventQueue.offer((SensorDataEvent)e);
            }
        }
    }
    
    
    @Override
    public void close()
    {
        for (ISensorDataInterface outputInterface: dataSources)
        {
            if (outputInterface.isPushSupported())
                outputInterface.unregisterListener(this);
        }
        
        eventQueue.clear();
    }
    
    
    @Override
    protected void finalize()
    {
        
    }
}
