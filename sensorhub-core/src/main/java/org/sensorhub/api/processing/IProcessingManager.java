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

package org.sensorhub.api.processing;

import java.util.List;
import java.util.concurrent.Future;
import javax.print.DocFlavor.URL;
import org.sensorhub.api.module.IModuleManager;


/**
 * <p>
 * Management interface for process implementations
 * </p>
 *
 * <p>Copyright (c) 2010</p>
 * @author Alexandre Robin
 * @since Nov 5, 2010
 */
public interface IProcessingManager extends IModuleManager<IProcess>
{
	/**
	 * @return the list of all process code configured on the system
	 */
	public List<String> getAllProcessCodePackages();
	
	
	/**
	 * Installs a process code package (jar file) from the specified URL for the process URI
	 * @param processURI URI of process type that this code should be used for
	 * @param codePackage URL of jar containing classes necessary to run the specified method
	 * @param replace if true, old code associated to the same URI will be destroyed 
	 */
	public void installProcessCode(String processURI, URL codePackage, boolean replace);
	
	
	/**
	 * Uninstalls process code with specified URI
	 * @param processURI
	 */
	public void uninstallProcessCode(String processURI);
	
	
    
    /**
     * Executes the specified process synchronously (method will block until processing is complete)
     * @param processID 
     */
    public void syncExec(String processID);
    
    
    /**
     * Executes the specified process in a separate thread with specified priority
     * (If too many processes are already running, process may actually be scheduled for later execution)
     * @param processID 
     * @param priority
     * @return Future object allowing the caller can cancel and know when process is actually terminated
     */
    public Future<?> asyncExec(String processID, int priority);
}
