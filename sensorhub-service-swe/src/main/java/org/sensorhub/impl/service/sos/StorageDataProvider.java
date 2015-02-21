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
import java.util.Iterator;
import java.util.List;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.persistence.DataKey;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IDataFilter;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.ITimeSeriesDataStore;
import org.sensorhub.api.sensor.SensorException;
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
 * Implementation of SOS data provider connecting to a storage via 
 * SensorHub's persistence API (ITimeSeriesStorage and derived classes)
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class StorageDataProvider implements ISOSDataProvider
{
    private static final long MAX_WAIT_TIME = 5000L;
    
    IBasicStorage<?> storage;
    List<StorageState> dataStoresStates;
    DataComponentFilter recordFilter;
    double replaySpeedFactor;
    double lastRecordTime = Double.NaN;
    long lastSystemTime; 
    
    
    class StorageState
    {
        ITimeSeriesDataStore<IDataFilter> dataStore;
        Iterator<? extends IDataRecord<DataKey>> recordIterator;
        IDataRecord<DataKey> nextRecord;        
    }
    
    
    public StorageDataProvider(IBasicStorage<?> storage, final SOSDataFilter filter)
    {
        this.storage = storage;
        this.dataStoresStates = new ArrayList<StorageState>();
        this.recordFilter = new DataComponentFilter(filter);
        this.replaySpeedFactor = filter.getReplaySpeedFactor();
        
        final double[] timePeriod = new double[] {
            filter.getTimeRange().getStartTime(),
            filter.getTimeRange().getStopTime()
        };
        
        // prepare record filter
        IDataFilter storageFilter = new IDataFilter() {

            @Override
            public double[] getTimeStampRange()
            {
                return timePeriod;
            }

            @Override
            public String getProducerID()
            {
                return null;
            }
        };
        
        // loop through all outputs and connect to the ones containing observables we need
        for (ITimeSeriesDataStore<IDataFilter> dataStore: storage.getDataStores().values())
        {
            // keep it if we can find one of the observables
            DataIterator it = new DataIterator(dataStore.getRecordDescription());
            while (it.hasNext())
            {
                String defUri = (String)it.next().getDefinition();
                if (filter.getObservables().contains(defUri))
                {
                    StorageState state = new StorageState();
                    state.dataStore = dataStore;
                    state.recordIterator = dataStore.getRecordIterator(storageFilter);
                    if (state.recordIterator.hasNext()) // prefetch first record
                        state.nextRecord = state.recordIterator.next();
                    dataStoresStates.add(state);
                    
                    // break for now since currently we support only requesting data from one store at a time
                    // TODO support case of multiple stores since it is technically possible with GetObservation
                    break;
                }
            }
        }
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
        obs.setProcedure(new ProcedureRef(storage.getLatestDataSourceDescription().getUniqueIdentifier()));
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
    public DataBlock getNextResultRecord()
    {
        double nextStorageTime = Double.POSITIVE_INFINITY;
        int nextStorageIndex = -1;
        
        // select data store with next earliest time stamp
        for (int i = 0; i < dataStoresStates.size(); i++)
        {
            StorageState state = dataStoresStates.get(i);          
            if (state.nextRecord == null)
                continue;                
            
            double recTime = state.nextRecord.getKey().timeStamp;
            if (recTime < nextStorageTime)
            {
                nextStorageTime = recTime;
                nextStorageIndex = i;
            }
        }
        
        if (nextStorageIndex < 0)
            return null;
        
        // get datablock from selected data store 
        StorageState state = dataStoresStates.get(nextStorageIndex);
        DataBlock datablk = state.nextRecord.getData();
        
        // prefetch next record
        if (state.recordIterator.hasNext())
            state.nextRecord = state.recordIterator.next();
        else
            state.nextRecord = null;
        
        // wait if replay mode is active
        if (!Double.isNaN(replaySpeedFactor))
        {
            if (!Double.isNaN(lastRecordTime))
            {
                long realEllapsedTime = System.currentTimeMillis() - lastSystemTime;
                long waitTime = (long)((nextStorageTime - lastRecordTime) * 1000. / replaySpeedFactor) - realEllapsedTime;
                if (waitTime > 0)
                {
                    if (waitTime > MAX_WAIT_TIME)
                        waitTime = MAX_WAIT_TIME;
                    try { Thread.sleep(waitTime ); }
                    catch (InterruptedException e) { }
                }
            }
            
            lastRecordTime = nextStorageTime;
            lastSystemTime = System.currentTimeMillis();
        }        
        
        // return record properly filtered according to selected observables
        return recordFilter.getFilteredRecord(state.dataStore.getRecordDescription(), datablk);
    }
    

    @Override
    public DataComponent getResultStructure()
    {
        // TODO generate choice if request includes several outputs
        
        return dataStoresStates.get(0).dataStore.getRecordDescription();
    }
    

    @Override
    public DataEncoding getDefaultResultEncoding()
    {
        return dataStoresStates.get(0).dataStore.getRecommendedEncoding();
    }


    @Override
    public void close()
    {
                
    }

}
