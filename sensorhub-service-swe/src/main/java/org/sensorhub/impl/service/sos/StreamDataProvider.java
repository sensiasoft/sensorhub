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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.Event;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.service.ServiceException;
import org.vast.data.DataIterator;
import org.vast.ogc.def.DefinitionRef;
import org.vast.ogc.gml.FeatureRef;
import org.vast.ogc.om.IObservation;
import org.vast.ogc.om.ObservationImpl;
import org.vast.ogc.om.ProcedureRef;
import org.vast.swe.SWEConstants;
import org.vast.util.TimeExtent;


/**
 * <p>
 * Implementation of SOS data provider connecting to a streaming data source
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public abstract class StreamDataProvider implements ISOSDataProvider, IEventListener
{
    IDataProducerModule<?> dataSource;
    List<IStreamingDataInterface> sourceOutputs;
    BlockingDeque<DataEvent> eventQueue;
    long timeOut;
    long stopTime;
    boolean latestRecordOnly;
    
    DataEvent lastDataEvent;
    int nextEventRecordIndex = 0;
            
    
    public StreamDataProvider(IDataProducerModule<?> dataSource, StreamDataProviderConfig config, SOSDataFilter filter) throws ServiceException
    {
        this.dataSource = dataSource;
        this.sourceOutputs = new ArrayList<IStreamingDataInterface>();
        this.eventQueue = new LinkedBlockingDeque<DataEvent>(1);
        
        // figure out stop time (if any)
        stopTime = ((long)filter.getTimeRange().getStopTime()) * 1000L;
        
        // get list of desired stream outputs
        dataSource.getConfiguration();
        
        // loop through all outputs and connect to the ones containing observables we need
        for (IStreamingDataInterface outputInterface: dataSource.getAllOutputs().values())
        {
            // skip hidden outputs
            if (config.hiddenOutputs != null && config.hiddenOutputs.contains(outputInterface.getName()))
                continue;
            
            // keep it if we can find one of the observables
            DataIterator it = new DataIterator(outputInterface.getRecordDescription());
            while (it.hasNext())
            {
                String defUri = (String)it.next().getDefinition();
                if (filter.getObservables().contains(defUri))
                {
                    // time out after a certain period if no sensor data is produced
                    timeOut = (long)(config.liveDataTimeout * 1000);
                    sourceOutputs.add(outputInterface);
                    
                    // break for now since we support only requesting data from one output at a time
                    // TODO support case of multiple outputs since it is technically possible with GetObservation
                    break; 
                }
            }
        }
        
        // if everything went well listen for events on the selected outputs
        for (final IStreamingDataInterface outputInterface: sourceOutputs)
        {
            // case of time instant = now, just return latest record
            if (isNowTimeInstant(filter.getTimeRange()))
            {
                DataBlock data = outputInterface.getLatestRecord();
                eventQueue.offerLast(new DataEvent(System.currentTimeMillis(), outputInterface, data));
                stopTime = Long.MAX_VALUE; // make sure stoptime does not cause us to return null
                timeOut = 0L;
                latestRecordOnly = true;
            }
            
            // otherwise register listener
            else
                outputInterface.registerListener(this);
        }
    }
    
    
    protected boolean isNowTimeInstant(TimeExtent timeFilter)
    {
        if (timeFilter.isTimeInstant() && timeFilter.isBaseAtNow())
            return true;
        
        return false;
    }
    
    
    @Override
    public IObservation getNextObservation() throws SensorHubException
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
        
        // observation property URI
        String obsPropDef = result.getDefinition();
        if (obsPropDef == null)
            obsPropDef = SWEConstants.NIL_UNKNOWN;
        
        // FOI
        AbstractFeature foi = dataSource.getCurrentFeatureOfInterest();
        if (dataSource instanceof IMultiSourceDataProducer)
        {
            String entityID = lastDataEvent.getRelatedEntityID();
            foi = ((IMultiSourceDataProducer) dataSource).getCurrentFeatureOfInterest(entityID);
        }
        
        String foiID;
        if (foi != null)
            foiID = foi.getUniqueIdentifier();
        else
            foiID = SWEConstants.NIL_UNKNOWN;
        
        // create observation object        
        IObservation obs = new ObservationImpl();
        obs.setFeatureOfInterest(new FeatureRef(foiID));
        obs.setObservedProperty(new DefinitionRef(obsPropDef));
        obs.setProcedure(new ProcedureRef(dataSource.getCurrentDescription().getUniqueIdentifier()));
        obs.setPhenomenonTime(phenTime);
        obs.setResultTime(resultTime);
        obs.setResult(result);
        
        return obs;
    }
    
    
    private DataComponent getNextComponent()
    {
        DataBlock data = getNextResultRecord();
        if (data == null)
            return null;
        
        DataComponent copyComponent = getResultStructure().copy();
        copyComponent.setData(data);
        return copyComponent;
    }
    

    @Override
    public DataBlock getNextResultRecord()
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
        if (!dataSource.isStarted())
            return false;
        
        boolean interfaceActive = false;
        for (IStreamingDataInterface source: sourceOutputs)
        {
            if (source.isEnabled()) {
                interfaceActive = true;
                break;
            }
        }
        
        return interfaceActive;
    }
    

    @Override
    public DataComponent getResultStructure()
    {
        // TODO generate choice if request includes several outputs
        
        return sourceOutputs.get(0).getRecordDescription();
    }
    

    @Override
    public DataEncoding getDefaultResultEncoding()
    {
        return sourceOutputs.get(0).getRecommendedEncoding();
    }
    
    
    @Override
    public void handleEvent(Event<?> e)
    {
        if (e instanceof DataEvent)
        {
            if (((DataEvent) e).getType() == DataEvent.Type.NEW_DATA_AVAILABLE)
            {
                // TODO there is no guarantee that records are processed in chronological order
                // this is because events may not be received in chronological order in the 1st place
                // it's not as simple as using a sorting queue because we never know when is the next event!
                // we could use the average sampling period to decide how much to wait to confirm the order
                eventQueue.offer((DataEvent)e);
            }
        }
    }
    
    
    @Override
    public void close()
    {
        if (!latestRecordOnly)
        {
            for (IStreamingDataInterface outputInterface: sourceOutputs)
                outputInterface.unregisterListener(this);
        }
        
        eventQueue.clear();
    }
}
