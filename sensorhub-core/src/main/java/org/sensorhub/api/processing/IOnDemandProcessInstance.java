/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.processing;

import java.util.Map;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.persistence.IDataFilter;


/**
 * <p>
 * Interface for on-demand process instances.<br/>
 * An instance may connect to an archive or streaming data source and MUST
 * generate data events for each new record produced on any of its output.<br/>
 * The output can be an aggregate (e.g. DataArray) and thus a single event may
 * be generated.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 22, 2015
 */
public interface IOnDemandProcessInstance
{
    
    /**
     * Executes the process synchronously with the given set of parameters and data filters
     * @param paramValues
     * @param dataFilters
     */
    public void execute(DataBlock paramValues, Map<String, IDataFilter> dataFilters);
    
    
    /**
     * Retrieves the list of data outputs
     * @return map of output names -> data interface objects
     * @throws SensorHubException 
     */
    public Map<String, ? extends IStreamingDataInterface> getOutputs() throws SensorHubException;

}
