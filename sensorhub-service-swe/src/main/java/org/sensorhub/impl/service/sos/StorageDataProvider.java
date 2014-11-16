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

import java.util.Iterator;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.persistence.IDataStorage;
import org.vast.ogc.om.IObservation;
import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataProvider;


/**
 * <p>
 * Implementation of SOS data provider connecting to a storage via 
 * SensorHub's persistence API (IDataStorage and derived classes)
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 7, 2013
 */
public class StorageDataProvider implements ISOSDataProvider
{
    IDataStorage<?,?,?> db;
    DataComponent srcDataDef;
    SOSDataFilter filter;
    Iterator<DataBlock> blkIterator;
    
    
    public StorageDataProvider(StorageDataProviderConfig config, SOSDataFilter filter)
    {
        this.srcDataDef = db.getRecordDescription();
        this.filter = filter;
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
        DataBlock srcDatablk = blkIterator.next();
        return selectObservables(this.srcDataDef, srcDatablk, this.filter);
    }
    

    @Override
    public DataComponent getResultStructure() throws Exception
    {
        return selectObservables(this.srcDataDef, this.filter);
    }
    

    @Override
    public DataEncoding getDefaultResultEncoding() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    
    private DataComponent selectObservables(DataComponent srcDataDef, SOSDataFilter filter)
    {
        
        
        return null;
    }
    
    
    private DataBlock selectObservables(DataComponent srcDataDef, DataBlock srcDatablk, SOSDataFilter filter)
    {
        
        
        return null;
    }


    @Override
    public void close()
    {
                
    }

}
