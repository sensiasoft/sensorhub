/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorEvent;
import org.sensorhub.api.sensor.SensorEvent.Type;
import org.sensorhub.api.sensor.SensorException;
import org.vast.data.DataIterator;
import org.vast.ogc.def.DefinitionRef;
import org.vast.ogc.gml.FeatureRef;
import org.vast.ogc.om.IObservation;
import org.vast.ogc.om.ObservationImpl;
import org.vast.ogc.om.ProcedureRef;
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Implementation of SOS data provider connecting to a sensor via 
 * SensorHub's sensor API (ISensorDataInterface)
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class SensorDataProvider implements ISOSDataProvider, IEventListener
{
    ISensorModule<?> sensor;
    List<ISensorDataInterface> dataSources;
    List<Timer> pollTimers;
    ISensorDataInterface nextInterface;
        
    
    public SensorDataProvider(ISensorModule<?> srcSensor, SOSDataFilter filter) throws SensorException
    {
        this.sensor = srcSensor;
        this.dataSources = new ArrayList<ISensorDataInterface>();
        this.pollTimers = new ArrayList<Timer>();
        
        // loop through all outputs connect to the ones containing observables we need
        for (final ISensorDataInterface outputInterface: sensor.getAllOutputs().values())
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
                    dataSources.add(outputInterface);
                    break;
                }
            }
            
            // register listener if push is supported
            if (outputInterface.isPushSupported())
            {
                outputInterface.registerListener(this);
            }
            
            // otherwise setup timer task to poll regularly
            // poll at twice the sampling rate
            else
            {
                final Object lock = this;
                TimerTask pollTask = new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        synchronized (lock)
                        {
                            nextInterface = outputInterface;
                            lock.notifyAll();
                        }                        
                    }                
                };
                
                Timer timer = new Timer(sensor.getName() + " Polling", true);
                timer.scheduleAtFixedRate(pollTask, 0, (long)(outputInterface.getAverageSamplingPeriod() * 500.));
            }
        }
    }
    
    
    @Override
    public IObservation getNextObservation() throws Exception
    {
        DataComponent result = getNextComponent();
        if (result == null)
            return null;
        
        // generate resultTime as now
        TimeExtent resultTime = new TimeExtent();
        resultTime.setBaseTime(new Date().getTime()/1000.);
        
        // TODO extract phenomenon time from record 'SamplingTime'
        TimeExtent phenTime = new TimeExtent();
        phenTime.setBaseTime(new Date().getTime()/1000.);
        
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
    

    @Override
    public DataBlock getNextResultRecord() throws SensorException
    {
        return getNextDataBlock();
    }
    

    @Override
    public DataComponent getResultStructure() throws SensorException
    {
        // TODO generate choice if request includes several outputs
        // not possible in core SOS because only one observed property can be requested at a time
        
        return dataSources.get(0).getRecordDescription();
    }
    

    @Override
    public DataEncoding getDefaultResultEncoding() throws Exception
    {
        return dataSources.get(0).getRecommendedEncoding();
    }


    private DataBlock getNextDataBlock() throws SensorException
    {
        DataBlock data = null;
        
        if (!hasMoreData())
            return null;
        
        while (data == null)
        {
            waitForNextObs();
            data = nextInterface.getLatestRecord();
        }
        
        return data;
    }
    
    
    private DataComponent getNextComponent() throws SensorException
    {
        DataBlock data = null;
        if ((data = getNextDataBlock()) == null)
            return null;
        
        DataComponent copyComponent = getResultStructure().copy();
        copyComponent.setData(data);
        return copyComponent;
    }
    
    
    private synchronized void waitForNextObs()
    {
        try
        {
            do { this.wait(); }
            while (nextInterface == null);
        }
        catch (InterruptedException e)
        {
        }
    }
    
    
    private boolean hasMoreData()
    {
        if (!sensor.isEnabled() || !sensor.isConnected())
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
    public void handleEvent(Event e)
    {
        if (e instanceof SensorEvent)
        {
            if (((SensorEvent) e).getType() == Type.NEW_DATA_AVAILABLE)
            {
                synchronized (this)
                {
                    nextInterface = (ISensorDataInterface)e.getSource();
                    this.notifyAll();
                }
            }
            else if (((SensorEvent) e).getType() == Type.DISCONNECTED)
            {
                
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
    }
    
    
    @Override
    protected void finalize()
    {
        
    }
}
