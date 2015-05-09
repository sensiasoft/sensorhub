/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.persistence.perst;

import java.util.HashMap;
import java.util.Map;
import net.opengis.sensorml.v20.AbstractProcess;
import org.garret.perst.Index;
import org.garret.perst.Persistent;
import org.garret.perst.Storage;


/**
 * <p>
 * PERST implementation of a basic record storage fed by a single producer
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since May 8, 2015
 */
class BasicStorageRoot extends Persistent
{
    Index<AbstractProcess> descriptionTimeIndex;
    Map<String, TimeSeriesImpl> dataStores;
    
    
    // default constructor needed on Android JVM
    BasicStorageRoot() {}
    
    
    public BasicStorageRoot(Storage db)
    {
        super(db);
        dataStores = new HashMap<String,TimeSeriesImpl>(10);
        descriptionTimeIndex = db.<AbstractProcess>createIndex(double.class, true);
    }
}
