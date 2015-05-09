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

import org.sensorhub.api.common.EntityEvent;
import org.sensorhub.api.data.FoiEvent.Type;
import net.opengis.gml.v32.AbstractFeature;


/**
 * <p>
 * Type of event generated when a new FOI is being targeted by a sensor or 
 * process. It is immutable and carries feature data by reference.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Apr 23, 2015
 */
public class FoiEvent extends EntityEvent<Type>
{
	/**
     * Possible event types for a FoiEvent
     */
    public enum Type
    {
        NEW_FOI,
        END_FOI
    };
    
    
    /**
	 * Description of Feature of Interest related to this event (by reference) 
	 */
	protected AbstractFeature foi;
	
	
	/**
	 * ID of feature of interest related to this event
	 */
	protected String foiID;
	
	
	/**
	 * Time at which the feature of interest starts being observed
	 */
	protected double startTime;
	
	
	protected boolean replacePreviousFoi;
	
	
	/**
	 * Creates a {@link Type#NEW_FOI} event
	 * @param timeStamp time of event generation (unix time in milliseconds, base 1970)
     * @param srcModule module that generated the event
	 */
	public FoiEvent(long timeStamp, IDataProducerModule<?> srcModule)
	{
	    this(timeStamp,
	         srcModule.getCurrentDescription().getUniqueIdentifier(),
	         srcModule);
	}
	
	
	public FoiEvent(long timeStamp, String sensorID, IDataProducerModule<?> srcModule)
    {
        this.type = Type.NEW_FOI;
        this.timeStamp = timeStamp;
        this.source = srcModule;
        this.relatedEntityID = sensorID;
    }
	
	
	public AbstractFeature getFoi()
    {
        return foi;
    }


    public String getFoiID()
    {
        return foiID;
    }

}
