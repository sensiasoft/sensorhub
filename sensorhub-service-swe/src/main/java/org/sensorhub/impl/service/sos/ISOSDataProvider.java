/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Initial Developer of the Original Code is SENSIA SOFTWARE LLC.
 Portions created by the Initial Developer are Copyright (C) 2012
 the Initial Developer. All Rights Reserved.

 Please Contact Alexandre Robin <alex.robin@sensiasoftware.com> for more
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.vast.ogc.om.IObservation;


/**
 * <p>
 * Interface to be implemented for providing data to the SOS engine.
 * One data provider is mapped for each SOS offering.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @date Nov 18, 2012
 * */
public interface ISOSDataProvider
{  
    
    /**
     * Requests provider to generate the next observation from the
     * underlying data, given the current config and filter
     * @return observation instance
     * @throws Exception
     */
    public IObservation getNextObservation() throws Exception;
    
    
    /**
     * Requests provider to generate the next CDM record from the
     * underlying data, given the current config and filter
     * @return data block
     * @throws Exception
     */
    public DataBlock getNextResultRecord() throws Exception;
    
    
    /**
     * Requests provider to provide the result structure corresponding
     * to the current config and filter
     * @return data component
     * @throws Exception 
     */
    public DataComponent getResultStructure() throws Exception;
    
    
    /**
     * Requests provider to specify the preferred encoding for the
     * underlying data, given the current config and filter
     * @return encoding instance
     * @throws Exception 
     */
    public DataEncoding getDefaultResultEncoding() throws Exception;
    
    
    /**
     * Properly releases all resources accessed by provider
     * (for instance, when connection is ended by client)
     */
    public void close();
}
