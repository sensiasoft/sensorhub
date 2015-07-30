/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sensorthings;

import java.util.ArrayList;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.service.HttpServer;


/**
 * <p>
 * Main module implementing SensorThings API.<br/>
 * The real work is done in the servlet.
 * </p>
 *
 * <p>Copyright (c) 2014 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Jul 15, 2015
 */
public class SensorThingsService extends AbstractModule<SensorThingsConfig>
{
    SensorThingsServlet servlet;
    
    
    public SensorThingsService()
    {        
    }    


    @Override
    public void start() throws SensorHubException
    {
        HttpServer httpServer = HttpServer.getInstance();
        if (httpServer == null)
            throw new RuntimeException("HTTP server must be started");
        
        if (!httpServer.isEnabled())
            return;
        
        if (servlet == null)
        {
            // build list of connected storages
            ModuleRegistry reg = SensorHub.getInstance().getModuleRegistry();
            ArrayList<IObsStorage> storageList = new ArrayList<IObsStorage>();
            for (String storageID: config.obsStorageIDs)
            {
                try
                {
                    storageList.add((IObsStorage)reg.getModuleById(storageID));
                }
                catch (Exception e)
                {
                    throw new SensorHubException("Cannot instantiate storage with ID " + storageID, e);
                }
            }
            
            // instantiate servlet with connected storage
            servlet = new SensorThingsServlet(storageList);
            
            // deploy servlet to HTTP server
            HttpServer.getInstance().deployServlet(servlet, config.endPoint);
        }
    }
    

    @Override
    public void stop() throws SensorHubException
    {
        HttpServer httpServer = HttpServer.getInstance();        
        if (httpServer == null || !httpServer.isEnabled())
            return;
        
        httpServer.undeployServlet(servlet);
        servlet = null;
    }
    

    @Override
    public void cleanup() throws SensorHubException
    {
        
    }

}
