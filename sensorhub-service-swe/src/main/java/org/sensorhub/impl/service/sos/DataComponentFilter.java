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

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.vast.ows.server.SOSDataFilter;


/**
 * <p>
 * Generates filtered data component description and data depending on what
 * observables are selected in the SOS request.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Nov 27, 2014
 */
public class DataComponentFilter
{
    final SOSDataFilter filter;
    
    
    public DataComponentFilter(SOSDataFilter filter)
    {
        this.filter = filter;
    }
    
    
    public DataComponent getFilteredRecordDescription(DataComponent recordDef)
    {
        return recordDef;
    }
    
    
    public DataBlock getFilteredRecord(DataComponent recordDef, DataBlock blk)
    {
        return blk;
    }
}
