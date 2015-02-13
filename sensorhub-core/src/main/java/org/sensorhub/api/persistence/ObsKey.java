/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.persistence;


/**
 * <p>
 * Interface for keys used to index observations
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 6, 2010
 */
public class ObsKey extends DataKey
{
    /**
     * ID of feature of interest (i.e. the feature that was the target of the oservation)
     */
    public String foiID;
    
    
    public ObsKey(String producerID, String foiID, long timeStamp)
    {
        super(producerID, timeStamp);
        this.foiID = foiID;
    }
}
