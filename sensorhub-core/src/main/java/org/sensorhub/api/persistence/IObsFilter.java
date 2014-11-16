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
     * Sampling time period from which to select observation
     */
    public TimeExtent getSamplingTimeRange();
    
    
    /**
     * List of features of interest
     */
    public List<String> getFoiIds();
    
    
    /**
     * List of data components/observed properties to retrieve from storage
     */
    public List<String> getObservedProperties();
    
    
    /**
     * Region of interest    
     */
    public Polygon getRoi();
}
