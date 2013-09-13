/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License Version
 1.1 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at
 http://www.mozilla.org/MPL/MPL-1.1.html
 
 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.
 
 The Original Code is "SensorHub".
 
 The Initial Developer of the Original Code is Sensia Software LLC.
 <http://www.sensiasoftware.com>. Portions created by the Initial
 Developer are Copyright (C) 2013 the Initial Developer. All Rights Reserved.
 
 Please contact Alexandre Robin <alex.robin@sensiasoftware.com> for more 
 information.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import org.vast.cdm.common.DataBlock;
import org.vast.cdm.common.DataComponent;
import org.vast.cdm.common.DataEncoding;
import org.vast.ogc.om.IObservation;
import org.vast.ows.sos.ISOSDataProvider;


/**
 * <p>
 * Implementation of SOS data provider connecting to a sensor via 
 * SensorHub's sensor API (ISensorDataInterface)
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class SensorDataProvider implements ISOSDataProvider
{

    public SensorDataProvider(SensorDataProviderConfig config)
    {
        
    }
    
    
    @Override
    public IObservation getNextObservation() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }
    

    @Override
    public DataBlock getNextResultRecord() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }
    

    @Override
    public DataComponent getResultStructure() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }
    

    @Override
    public DataEncoding getDefaultResultEncoding() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

}
