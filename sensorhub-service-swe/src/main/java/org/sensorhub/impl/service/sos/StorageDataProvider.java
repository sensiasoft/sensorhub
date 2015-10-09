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
import java.util.Map.Entry;
import java.util.Set;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.persistence.IBasicStorage;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IObsFilter;
import org.sensorhub.api.persistence.IRecordStoreInfo;
import org.sensorhub.api.persistence.ObsFilter;
import org.sensorhub.api.persistence.ObsKey;
import org.sensorhub.api.sensor.SensorException;
import org.vast.data.DataIterator;
import org.vast.ogc.def.DefinitionRef;
import org.vast.ogc.gml.FeatureRef;
import org.vast.ogc.om.IObservation;
import org.vast.ogc.om.ObservationImpl;
import org.vast.ogc.om.ProcedureRef;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.ows.sos.SOSDataFilter;
import org.vast.swe.SWEConstants;
import org.vast.util.TimeExtent;
import com.vividsolutions.jts.geom.Polygon;


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
    IBasicStorage storage;
    List<StorageState> dataStoresStates;
    String foiID;
    
    // replay stuff 
    double replaySpeedFactor;
    double requestStartTime;
    long requestSystemTime;
    
    
    class StorageState
    {
        IRecordStoreInfo recordInfo;
        Iterator<? extends IDataRecord> recordIterator;
        IDataRecord nextRecord;
    }
    
    
    public StorageDataProvider(IBasicStorage storage, StorageDataProviderConfig config, final SOSDataFilter filter)
    {
        this.storage = storage;
        this.dataStoresStates = new ArrayList<StorageState>();
        this.replaySpeedFactor = filter.getReplaySpeedFactor();
        this.requestSystemTime = System.currentTimeMillis();
                        
        // prepare time range filter
        final double[] timePeriod;
        if (filter.getTimeRange() != null && !filter.getTimeRange().isNull())
        {
            timePeriod = new double[] {
                filter.getTimeRange().getStartTime(),
                filter.getTimeRange().getStopTime()
            };
            
            this.requestStartTime = timePeriod[0];
        }
        else
            timePeriod = null;
        
        // loop through all outputs and connect to the ones containing observables we need
        for (Entry<String, ? extends IRecordStoreInfo> dsEntry: storage.getRecordStores().entrySet())
        {
            // skip hidden outputs
            if (config.hiddenOutputs != null && config.hiddenOutputs.contains(dsEntry.getKey()))
                continue;
            
            IRecordStoreInfo recordInfo = dsEntry.getValue();
            String recordType = recordInfo.getName();
            
            // keep it if we can find one of the observables
            DataIterator it = new DataIterator(recordInfo.getRecordDescription());
            while (it.hasNext())
            {
                String defUri = (String)it.next().getDefinition();
                if (filter.getObservables().contains(defUri))
                {
                    // prepare record filter
                    IObsFilter storageFilter = new ObsFilter(recordType) {
                        public double[] getTimeStampRange() { return timePeriod; }
                        public Set<String> getFoiIDs() { return filter.getFoiIds(); }
                        public Polygon getRoi() {return filter.getRoi(); }
                    };
                    
                    StorageState state = new StorageState();
                    state.recordInfo = recordInfo;
                    state.recordIterator = storage.getRecordIterator(storageFilter);
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
        
        // observation property URI
        String obsPropDef = result.getDefinition();
        if (obsPropDef == null)
            obsPropDef = SWEConstants.NIL_UNKNOWN;
        
        // FOI
        String foiID = this.foiID;
        if (foiID == null)
            foiID = SWEConstants.NIL_UNKNOWN;
        
        // create observation object
        IObservation obs = new ObservationImpl();
        obs.setFeatureOfInterest(new FeatureRef(foiID));
        obs.setObservedProperty(new DefinitionRef(obsPropDef));
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
        IDataRecord nextRec = state.nextRecord;
        DataBlock datablk = nextRec.getData();
        
        // also save FOI ID if set
        if (nextRec.getKey() instanceof ObsKey)
            this.foiID = ((ObsKey)nextRec.getKey()).foiID;
                
        // prefetch next record
        if (state.recordIterator.hasNext())
            state.nextRecord = state.recordIterator.next();
        else
            state.nextRecord = null;
        
        // wait if replay mode is active
        if (!Double.isNaN(replaySpeedFactor))
        {
            long realEllapsedTime = System.currentTimeMillis() - requestSystemTime;
            long waitTime = (long)((nextStorageTime - requestStartTime) * 1000. / replaySpeedFactor) - realEllapsedTime;
            if (waitTime > 0)
            {
                try { Thread.sleep(waitTime ); }
                catch (InterruptedException e) { }
            }
        }        
        
        // return record properly filtered according to selected observables
        return datablk;
    }
    

    @Override
    public DataComponent getResultStructure()
    {
        // TODO generate choice if request includes several outputs
        
        return dataStoresStates.get(0).recordInfo.getRecordDescription();
    }
    

    @Override
    public DataEncoding getDefaultResultEncoding()
    {
        return dataStoresStates.get(0).recordInfo.getRecommendedEncoding();
    }


    @Override
    public void close()
    {
                
    }

}
