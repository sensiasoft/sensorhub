/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.test.processing;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.processing.DataSourceConfig;
import org.sensorhub.api.processing.ProcessException;
import org.sensorhub.api.processing.StreamProcessConfig;
import org.sensorhub.impl.processing.AbstractStreamProcess;
import org.vast.process.DataQueue;


public class DummyProcessAutoIO extends AbstractStreamProcess<StreamProcessConfig>
{
    public static final String OUTPUT_PREFIX = "processed_";
    
    
    public DummyProcessAutoIO()
    {        
    }
    
    
    @Override
    public void init(StreamProcessConfig config) throws SensorHubException
    {
        super.init(config);
        
        // create one output with same struct as each input
        for (DataComponent inputDef: inputs.values())
        {
            DataComponent outputDef = inputDef.copy();
            outputDef.setName(OUTPUT_PREFIX + outputDef.getName());
            addOutput(new DummyOutput(this, outputDef));
        }
    }
    
    
    @Override
    protected void process(DataEvent lastEvent) throws ProcessException
    {
        try
        {
            IStreamingDataInterface srcInterface = lastEvent.getSource();
            
            for (DataQueue q: streamSources.get(srcInterface).getDataQueues())
            {
                if (q.isDataAvailable())
                {
                    DataBlock newData = q.get().clone();
                    
                    // multiply everything by 2
                    // assuming all data in record is numerical
                    for (int i=0; i<newData.getAtomCount(); i++)
                        newData.setDoubleValue(i, newData.getDoubleValue(i) * 2.0);
                    
                    String outputName = OUTPUT_PREFIX + q.getDestinationComponent().getName();
                    ((DummyOutput)outputInterfaces.get(outputName)).sendOutput(newData);
                }
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public boolean isCompatibleDataSource(DataSourceConfig dataSource)
    {
        return true;
    }
    
    
    @Override
    public boolean isPauseSupported()
    {
        return false;
    }
}
