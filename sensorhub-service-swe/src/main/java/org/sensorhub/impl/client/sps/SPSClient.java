/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.client.sps;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.common.SensorHubException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.ows.OWSException;
import org.vast.ows.sps.DescribeTaskingRequest;
import org.vast.ows.sps.DescribeTaskingResponse;
import org.vast.ows.sps.SPSUtils;
import org.vast.ows.sps.SubmitRequest;
import org.vast.ows.sps.SubmitResponse;


/**
 * <p>
 * Implementation of an SPS client that forwards command data records to
 * a remote sensor.<br/>
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Sep 5, 2015
 */
public class SPSClient
{
    protected static final Logger log = LoggerFactory.getLogger(SPSClient.class);
    
    SPSUtils utils = new SPSUtils();
    DescribeTaskingResponse dtResp;
    SubmitRequest submitReq;
    String spsEndpoint;
    String version;
    String sensorUID;
    
    
    
    public SPSClient(String spsEndpoint, String version, String sensorUID)
    {
        this.spsEndpoint = spsEndpoint;
        this.version = version;
        this.sensorUID = sensorUID;
    }
    
    
    public void retrieveCommandDescription() throws SensorHubException
    {
        try
        {
            // connect to SPS server and retrieve tasking parameters
            DescribeTaskingRequest dtReq = new DescribeTaskingRequest();
            dtReq.setVersion(version);
            dtReq.setPostServer(spsEndpoint);
            dtReq.setProcedureID(sensorUID);
            utils.writeXMLQuery(System.out, dtReq);
            dtResp = utils.sendRequest(dtReq, false);
            utils.writeXMLResponse(System.out, dtResp);
            
            // create submit request template
            submitReq = new SubmitRequest();
            submitReq.setVersion(version);
            submitReq.setPostServer(spsEndpoint);
            submitReq.setProcedureID(sensorUID);
        }
        catch (OWSException e)
        {
            throw new SensorHubException("Error while retrieving tasking message definition from SPS", e);
        }
    }
    
    
    public synchronized SubmitResponse sendTaskMessage(DataBlock data) throws SensorHubException
    {
        try
        {            
            submitReq.getParameters().clearData();
            submitReq.getParameters().addData(data);
            //utils.writeXMLQuery(System.out, submitReq);
            return (SubmitResponse)utils.sendRequest(submitReq, false);
        }
        catch (OWSException e)
        {
            throw new SensorHubException("Error while sending task to SPS", e);
        }
    }
    
    
    public DataComponent getCommandDescription()
    {
        return dtResp.getTaskingParameters();
    }

}
