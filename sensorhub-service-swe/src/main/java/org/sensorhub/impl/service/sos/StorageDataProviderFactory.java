/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2013 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sos;

import org.vast.ows.server.SOSDataFilter;
import org.vast.ows.sos.ISOSDataProvider;
import org.vast.ows.sos.SOSOfferingCapabilities;
import org.vast.sensorML.SMLProcess;
import org.vast.util.DateTime;


/**
 * <p>
 * Factory for storage data providers.
 * </p>
 * <p>
 * This factory is associated to an SOS offering and is persistent
 * throughout the lifetime of the service, so it must be threadsafe.
 * </p>
 * <p>
 * However, the server will obtain a new data provider instance from this
 * factory for each incoming request so the providers themselves don't need
 * to be threadsafe. 
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Sep 15, 2013
 */
public class StorageDataProviderFactory implements IDataProviderFactory
{
    StorageDataProviderConfig config;
    
    
    protected StorageDataProviderFactory(StorageDataProviderConfig config)
    {
        this.config = config;
    }
    
    
    @Override
    public SOSOfferingCapabilities generateCapabilities()
    {
        SOSOfferingCapabilities offering = new SOSOfferingCapabilities();
        
        return offering;
    }
    
    
    @Override
    public ISOSDataProvider getNewProvider(SOSDataFilter filter) throws Exception
    {
        
        return null;
    }


    @Override
    public SMLProcess generateSensorMLDescription(DateTime t)
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean isEnabled()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public void cleanup()
    {
        // TODO Auto-generated method stub
        
    }

}
