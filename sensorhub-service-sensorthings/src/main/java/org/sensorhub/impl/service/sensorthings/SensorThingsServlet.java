/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are Copyright (C) 2014 Sensia Software LLC.
 All Rights Reserved.
 
 Contributor(s): 
    Alexandre Robin <alex.robin@sensiasoftware.com>
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sensorthings;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.persistence.IDataRecord;
import org.sensorhub.api.persistence.IObsStorage;
import org.sensorhub.api.persistence.IRecordInfo;
import org.sensorhub.api.persistence.ObsFilter;
import org.sensorhub.api.persistence.ObsKey;


/**
 * <p>
 * Main servlet class implementing SensorThings API by parsing resource paths
 * and generating the proper JSON resources.
 * This can make use of the OData java library I assume.
 * </p>
 *
 * <p>Copyright (c) 2014 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Jul 15, 2015
 */
public class SensorThingsServlet extends HttpServlet
{
    private static final long serialVersionUID = 9007234798586560370L;
    List<IObsStorage> storageList;
    

    protected SensorThingsServlet(List<IObsStorage> storageList)
    {
        this.storageList = storageList;
        
        // TODO pre-compute ordered lists of sensors, datastreams, observationProperties, observations
        // so they can be retrieved faster later on in the doXXX methods
    }
    
    
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        super.doDelete(req, resp);
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        super.doGet(req, resp);
        
        // TODO parse resource path
        
        // ex: path = /Sensors
        // TODO get the list from the list of connected storages
        
        // ex: path = /Sensors{1}
        AbstractProcess p = storageList.get(0).getLatestDataSourceDescription();
        p.getName();
        p.getDescription();
        // TODO build resource from metadata contained in p
        
        // ex: path = /Sensors{1}/DataStreams
        for (IRecordInfo streamInfo: storageList.get(0).getRecordTypes().values())
        {
            DataComponent rec = streamInfo.getRecordDescription();
            double[] timeRange = storageList.get(0).getRecordsTimeRange(streamInfo.getRecordType());
            // TODO build datastream JSON resource from metadata contained in   
        }
        
        // ex: path = /Sensors{1}/DataStreams{3}/Observations
        // figure out name of datastream at index 3
        int i = 0;
        for (IRecordInfo streamInfo: storageList.get(0).getRecordTypes().values())
        {
            if (i == 2)
            {
                ObsFilter filter = new ObsFilter(streamInfo.getRecordType());
                Iterator<? extends IDataRecord> it = storageList.get(0).getRecordIterator(filter);
                while (it.hasNext())
                {
                    IDataRecord rec = it.next();
                    double time = rec.getKey().timeStamp;
                    String foi = ((ObsKey)rec.getKey()).foiID;
                    double scalarValue = rec.getData().getDoubleValue();// need to extract info from datablock for multidatastreams
                    // TODO build observation JSON resource for record info
                }
                break;
            }
        }   

        // TODO write JSON to HttpServletResponse
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // TODO Auto-generated method stub
        super.doPost(req, resp);
    }


    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // TODO Auto-generated method stub
        super.doPut(req, resp);
    }
}
