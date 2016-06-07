/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.module;

import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.common.IEventProducer;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent.ModuleState;


/**
 * <p>
 * Generic interface for all modules in the system.
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @param <ConfigType> 
 * @since Nov 12, 2010
 */
public interface IModule<ConfigType extends ModuleConfig> extends IEventProducer
{    
    
    /**
     * Sets the module configuration
     * @param config
     */
    public void setConfiguration(ConfigType config);
    
    
    /**
     * Retrieves a copy of the module configuration
     * (i.e. for reading only since changes won't have any effect until updateConfig is called)
     * @return a copy of the configuration object associated to this module
     */
    public ConfigType getConfiguration();
    
    
    // so that a module can change values allowed for a given configuration
    // option dynamically depending on its own state
    //public DataConstraint getConfigFieldConstraint(String fieldName);
    
    
    /**
     * Helper method to get the module's name
     * @return name string
     */
    public String getName();
    
    
    /**
     * Helper method to get the module's local ID
     * @return id string
     */
    public String getLocalID();
    
    
    /**
     * Checks if module is initialized
     * @return true if module is initialized, false otherwise
     */
    public boolean isInitialized();
 
    
    /**
     * Checks if module is started
     * @return true if module is started, false otherwise
     */
    public boolean isStarted();
    
    
    /**
     * @return the current state of the module
     */
    public ModuleState getCurrentState();
    
    
    /**
     * @return the current status message
     */
    public String getStatusMessage();
    
    
    /**
     * @return the last error that occured executing the module
     */
    public Throwable getCurrentError();
    
    
    /**
     * Requests to initialize the module with the specified configuration.<br/>
     * Implementations of this method block until the module is initialized or
     * return immediately while they wait for the proper init conditions.
     * @param config 
     * @throws SensorHubException
     */
    public void requestInit(ConfigType config) throws SensorHubException;
    
    
    /**
     * Initializes the module synchronously with the specified configuration.<br/>
     * Implementations of this method must block until the module is
     * successfully initialized or send an exception.<br/>
     * Module lifecycle events may not be generated when calling this method directly.<br/>
     * @param config
     * @throws SensorHubException 
     */
    public void init(ConfigType config) throws SensorHubException;
    
    
    /**
     * Updates the module's configuration dynamically.<br/>
     * The module must honor this new configuration unless an error is detected.
     * It is the responsability of the module to initiate a restart if the new
     * configuration requires it.
     * @param config
     * @throws SensorHubException 
     */
    public void updateConfig(ConfigType config) throws SensorHubException;
    
    
    /**
     * Requests the module to start.<br/>
     * Implementations of this method may block until the module is started or
     * return immediately while they wait for the proper start conditions.
     * @throws SensorHubException
     */
    public void requestStart() throws SensorHubException;
    
    
    /**
     * Starts the module synchronously with the current configuration.<br/>
     * Implementations of this method must block until the module is
     * successfully started or send an exception.<br/>
     * Module lifecycle events may not be generated when calling this method directly.<br/>
     * init() should always be called before start().
     * @throws SensorHubException
     */
    public void start() throws SensorHubException;
    
    
    /**
     * Requests the module to stop.<br/>
     * Implementations of this method may block until the module is stopped or
     * return immediately while they wait for the proper stop conditions.
     * @throws SensorHubException
     */
    public void requestStop() throws SensorHubException;
    
    
    
    /**
     * Stops the module.<br/>
     * All temporary resources created by the module should be cleaned
     * when this is called (ex: memory, files, connections, etc.)<br/>
     * Implementations of this method must block until the module is
     * successfully stopped or send an exception.<br/>
     * Module lifecycle events may not be generated when calling this method directly.<br/>
     * stop() can be called right after init() even if start() hasn't been called.
     * @throws SensorHubException
     */
    public void stop() throws SensorHubException;
    
    
    /**
     * Saves the state of this module.<br/> 
     * Implementations of this method must block until the module state is
     * successfully saved or send an exception.
     * @param saver
     * @throws SensorHubException 
     */
    public void saveState(IModuleStateManager saver) throws SensorHubException;
    
    
    /**
     * Restores the state of this module<br/>
     * Implementations of this method must block until the module state is
     * successfully loaded or send an exception.
     * @param loader
     * @throws SensorHubException 
     */
    public void loadState(IModuleStateManager loader) throws SensorHubException;
    
    
    /**
     * Cleans up all ressources used by the module when deleted
     * All persistent resources created by the module should be cleaned
     * when this is called
     * @throws SensorHubException
     */
    public void cleanup() throws SensorHubException;
    
    
    /**
     * Registers a listener to receive events generated by this module.br/>
     * When this method is called, the current state of the module is also notified
     * synchronously to guarantee that the listener always receives it.
     * @param listener
     */
    @Override
    public void registerListener(IEventListener listener);
    
    
    /**
     * Unregisters a listener and thus stop receiving events generayed by this module
     * @param listener
     */
    @Override
    public void unregisterListener(IEventListener listener);    
}
