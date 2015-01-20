/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
The Initial Developer is Sensia Software LLC. Portions created by the Initial
Developer are Copyright (C) 2014 the Initial Developer. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sps;

import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.service.ServiceException;
import org.vast.ows.sps.SPSOfferingCapabilities;
import net.opengis.swe.v20.DataBlock;


/**
 * <p>
 * SPS connector for handling commands sent to sensors.
 * This connector supports priorities, scheduling and persistent task management
 * </p>
 *
 * <p>Copyright (c) 2015</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Jan 16, 2015
 */
public class SharedSensorConnector extends DirectSensorConnector implements ISPSConnector
{
    
    
    public SharedSensorConnector(SensorConnectorConfig config) throws SensorHubException
    {
        super(config);
    }
    

    @Override
    public SPSOfferingCapabilities generateCapabilities() throws ServiceException
    {
        return super.generateCapabilities();
    }
    
    
    @Override
    public void updateCapabilities() throws Exception
    {
        super.updateCapabilities();        
    }
    
    
    @Override
    public void sendSubmitData(ITask task, DataBlock data) throws ServiceException
    {
        checkEnabled();     
        
        /*// time tag initial report
        StatusReport taskStatus = newTask.getStatusReport();
        taskStatus.setRequestStatus(RequestStatus.Pending);
        taskStatus.touch();     
        
        // runnable to process task asynchronously
        Runnable updateRunnable = new Runnable()
        {
            public void run()
            {
                // generate new task report
                ITask newTask = taskDB.getTask(taskID);
                StatusReport report = newTask.getStatusReport().clone();
                report.setTitle("Tasking Report");
                
                // launch feasibility study
                FeasibilityResult result = null;
                try
                {
                    // TODO if feasibility level = COMPLETE check or simulate conflicts
                    result = doFeasibilityStudy(request);
                }
                catch (SPSException e)
                {
                    e.printStackTrace(); // TODO log
                    return;
                }
                
                // if not feasible set status to rejected
                if (!isFeasible(result))
                {
                    report.setRequestStatus(RequestStatus.Rejected);
                    report.setEstimatedToC(null);
                    
                    // send notification
                    notifSystem.notifyRequestRejected(report);
                }
                else
                {
                    
                }
                                
                // commit to DB
                report.touch();
                taskDB.updateTaskStatus(report);
                
                // send notification
                notifSystem.notifyRequestAccepted(report);
            }
        };
                
        // start async processing (i.e. feasibility)
        Thread updateThread = new Thread(updateRunnable);
        updateThread.start();
        
        // wait 5s for definite accepted/rejected answer
        Thread.sleep(5000);*/
    }
}
