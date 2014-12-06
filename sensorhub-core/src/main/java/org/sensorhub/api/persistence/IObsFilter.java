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

package org.sensorhub.api.persistence;

import java.util.List;
import org.vast.util.TimeExtent;
import com.vividsolutions.jts.geom.Polygon;


/**
 * <p>
 * Simple structure for defining filtering criteria when retrieving observations from storage.
 * These criteria correspond to properties of the O&M model.
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 5, 2010
 */
public interface IObsFilter extends IDataFilter
{
    /**
     * @return the sampling time period from which to retrieve observations
     */
    public TimeExtent getSamplingTimeRange();
    
    
    /**
     * @return the list of features of interest to retrieve observations of
     */
    public List<String> getFoiIds();
    
    
    /**
     * @return the list of observed properties to retrieved data for
     */
    public List<String> getObservedProperties();
    
    
    /**
     * @return the region of interest from which to retrieve observations
     */
    public Polygon getRoi();
}
