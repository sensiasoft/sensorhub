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

package org.sensorhub.impl.persistence;

import java.util.List;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleStateLoader;
import org.sensorhub.api.module.IModuleStateSaver;
import org.sensorhub.api.processing.IProcess;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.impl.common.AbstractSimpleProcess;
import org.vast.cdm.common.DataComponent;


/**
 * <p>
 * This class allows for connecting the output of a sensor to a storage instance for
 * recording all data.
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Aug 30, 2013
 */
public class SimpleSensorStorageHelper extends AbstractSimpleProcess implements IProcess
{
    
    public SimpleSensorStorageHelper(String sensorID, List<String> outputNames)
    {
        // register to sensor data interface listener
        
        
        // setup storage (only activate if already created)
        
        // 
    }
    

    @Override
    public DataComponent getInputList()
    {
        // TODO Auto-generated method stub
        return null;
    }
    

    @Override
    public DataComponent getOutputList()
    {
        // TODO Auto-generated method stub
        return null;
    }
    

    @Override
    public DataComponent getParameterList()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    
    @Override
    public void saveState(IModuleStateSaver saver)
    {
        // TODO Auto-generated method stub
        
    }
    

    @Override
    public void loadState(IModuleStateLoader loader)
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void updateConfig(ProcessConfig config)
    {
        // TODO Auto-generated method stub
        
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {
        // TODO Auto-generated method stub
        
    }

}
