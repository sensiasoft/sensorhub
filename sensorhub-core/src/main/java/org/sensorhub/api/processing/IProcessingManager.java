/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.processing;

import java.util.Collection;
import java.util.concurrent.Future;
import javax.print.DocFlavor.URL;
import org.sensorhub.api.module.IModuleManager;


/**
 * <p>
 * Management interface for process implementations
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Nov 5, 2010
 */
public interface IProcessingManager extends IModuleManager<IProcessModule<?>>
{
	/**
	 * @return the list of all process code configured on the system
	 */
	public Collection<String> getAllProcessCodePackages();
	
	
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
