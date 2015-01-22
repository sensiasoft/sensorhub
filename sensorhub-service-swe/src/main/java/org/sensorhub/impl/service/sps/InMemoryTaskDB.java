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

import java.util.Hashtable;
import java.util.UUID;
import org.vast.ows.sps.FeasibilityReport;
import org.vast.ows.sps.GetFeasibilityRequest;
import org.vast.ows.sps.StatusReport;
import org.vast.ows.sps.SubmitRequest;
import org.vast.ows.sps.TaskingRequest;
import org.vast.ows.sps.StatusReport.RequestStatus;
import org.vast.util.DateTime;


public class InMemoryTaskDB implements ITaskDB
{
	public static String TASK_ID_PREFIX = "urn:sensorhub:sps:task:";
	public static String FEASIBILITY_ID_PREFIX = "urn:sensorhub:sps:feas:";
	protected Hashtable<String, Task> taskTable;
	
	
	public InMemoryTaskDB()
	{
		this.taskTable = new Hashtable<String, Task>();
	}
	
	
	public synchronized Task createNewTask(TaskingRequest request)
	{
		if (request instanceof SubmitRequest)
			return createNewTask((SubmitRequest)request);
		else if (request instanceof GetFeasibilityRequest)
			return createNewTask((GetFeasibilityRequest)request);
		else
			throw new IllegalStateException("Cannot create task for operation " + request.getOperation());
	}
	
	
	protected Task createNewTask(SubmitRequest request)
	{
		Task newTask = new Task();
		newTask.setRequest(request);
		String taskID = TASK_ID_PREFIX + UUID.randomUUID().toString();
		
		// initial status
		newTask.getStatusReport().setTaskID(taskID);
		newTask.getStatusReport().setTitle("Tasking Request Report");
		newTask.getStatusReport().setSensorID(request.getSensorID());
		newTask.getStatusReport().setRequestStatus(RequestStatus.Pending);
		
		// creation time
		newTask.setCreationTime(new DateTime());
		
		taskTable.put(taskID, newTask);
		
		return newTask;
	}
	
	
	protected Task createNewTask(GetFeasibilityRequest request)
	{
		Task newTask = new Task();
		newTask.setStatusReport(new FeasibilityReport());
		newTask.setRequest(request);
		String taskID = FEASIBILITY_ID_PREFIX + UUID.randomUUID().toString();
		
		// initial status
		newTask.getStatusReport().setTaskID(taskID);
		newTask.getStatusReport().setTitle("Feasibility Study Report");
		newTask.getStatusReport().setSensorID(request.getSensorID());
		newTask.getStatusReport().setRequestStatus(RequestStatus.Pending);
		
		// creation time
		newTask.setCreationTime(new DateTime());
		
		taskTable.put(taskID, newTask);
		
		return newTask;
	}


	public Task getTask(String taskID)
	{
		return taskTable.get(taskID);
	}


	public StatusReport getTaskStatus(String taskID)
	{
		Task task = taskTable.get(taskID);
		if (task == null)
			return null;
		
		return task.getStatusReport();
	}


	public StatusReport getTaskStatusSince(String taskID, DateTime date)
	{
		// TODO Auto-generated method stub
		return null;
	}


	public void updateTaskStatus(StatusReport report)
	{
		Task task = taskTable.get(report.getTaskID());
		if (task == null)
			return;
		
		task.setStatusReport(report);
	}


	public void close()
	{	
	}
}
