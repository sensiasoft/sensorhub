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

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.IEventProducer;
import org.sensorhub.api.module.IModule;


/**
 * <p>
 * Generic interface for all data producers using the SWE model to describe
 * structure and encoding of data they generate (e.g. sensors, processes...)
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 21, 2015
 */
public interface IStreamingDataInterface extends IEventProducer
{

    /**
     * Allows by-reference access to parent module
     * @return parent module instance
     */
    public IModule<?> getParentModule();
    
    
    /**
     * @return name of this producer interface
     */
    public String getName();


    /**
     * Checks if this producer is enabled
     * @return true if producer is enabled, false otherwise
     */
    public boolean isEnabled();
    
    
    /**
     * Retrieves the record definition for this producer.
     * <p><i>Note that this is usually sent by reference and MUST not be modified
     * by the caller. If you really need to modify it, first get an independent
     * copy using {@link net.opengis.swe.v20.DataComponent#copy()}</i></p>
     * @return a DataComponent object defining the structure of the produced data
     */
    public DataComponent getRecordDescription();


    /**
     * Provides the recommended encoding for this producer.
     * <p><i>Note that this is usually sent by reference and MUST not be modified
     * by the caller. If you really need to modify it, first get an independent
     * copy using {@link net.opengis.swe.v20.DataEncoding#copy()}</i></p>
     * @return recommended encoding description for the produced data
     */
    public DataEncoding getRecommendedEncoding();
}
