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

import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.processing.StreamProcessConfig;


public class DummyProcessAutoIO extends DummyProcessFixedIO
{
    
    public DummyProcessAutoIO()
    {        
    }
    
    
    @Override
    public void init(StreamProcessConfig config) throws SensorHubException
    {
        this.config = config;
    }


    @Override
    public void start() throws SensorHubException
    {
        super.start();
                
        // create one output with same struct as each input
        outputInterfaces.clear();
        for (DataComponent inputDef: inputs.values())
        {
            DataComponent outputDef = inputDef.copy();
            outputDef.setName(OUTPUT_PREFIX + outputDef.getName());
            addOutput(new DummyOutput(this, outputDef));
        }
    }
}
