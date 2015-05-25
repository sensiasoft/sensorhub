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

import java.util.Iterator;
import net.opengis.gml.v32.AbstractFeature;


/**
 * <p>
 * Adds support for storing/retrieving features of interests.<br/>
 * {@link ObsKey} and {@link IObsFilter} can be used in all record retrieval
 * methods and concrete implementations must handle them properly (e.g. they
 * must handle all obs filter criteria when they are specified)
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since April 25, 2015
 */
public interface IObsStorage extends IBasicStorage
{    
    
    /**
     * @return Number of features of interest registered in this storage
     */
    int getNumFois();
    
    
    /**
     * @return Iterator over IDs of features of interest for which observations
     * are available in this storage
     */
    public Iterator<String> getFoiIDs();
    
    
    /**
     * Retrieves a feature of interest by unique ID
     * @param uid unique ID of desired feature
     * @return The feature object or null if none exist with the given UID
     */
    public AbstractFeature getFoi(String uid);
    
    
    /**
     * Retrieves features of interest matching the given filter 
     * @param filter feature filter
     * @return Iterator over matching FOIs
     */
    public Iterator<AbstractFeature> getFois(IFeatureFilter filter);
    
    
    /**
     * Stores a new feature of interest description into storage.
     * @param foi feature object to store
     */
    public void storeFoi(AbstractFeature foi);
    
}
