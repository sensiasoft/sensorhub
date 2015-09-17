/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.data;

import java.util.Collection;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.sensorml.v20.AbstractProcess;


public interface IMultiSourceDataProducer
{

    /**
     * @return List of entity IDs for which this module produces data
     */
    public Collection<String> getEntityIDs();
    
    
    /**
     * Retrieves the most current SensorML description of the given entity.
     * @param entityID unique ID of the desired entity (e.g. sensor in a network)
     * @return AbstractProcess SensorML description of the data producing entity
     * or null if no description is available
     */
    public AbstractProcess getCurrentDescription(String entityID);
    
    
    /**
     * Used to check when SensorML description of the given entity was last updated.
     * This is useful to avoid requesting the object when it hasn't changed.
     * @param entityID unique ID of the desired entity (e.g. sensor in a network)
     * @return Date/time of last description update as julian time (1970) or
     * {@link Long#MIN_VALUE} if description was never updated.
     */
    public double getLastDescriptionUpdate(String entityID);
    
    
    /**
     * Retrieves the feature of interest for which the given entity is 
     * currently generating data.<br/>
     * @param entityID unique ID of the desired entity (e.g. sensor in a network)
     * @return Feature object
     */
    public AbstractFeature getCurrentFeatureOfInterest(String entityID);
    
    
    /**
     * @return Collection all features of interest for which this producer
     * is generating data.
     */
    public Collection<? extends AbstractFeature> getFeaturesOfInterest();
    
    
    /**
     * @return Collection of IDs of all features of interest for which this
     * producer is generating data.
     */
    public Collection<String> getFeaturesOfInterestIDs();
}
