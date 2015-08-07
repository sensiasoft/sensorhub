/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import javax.print.DocFlavor.URL;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.processing.IProcessingManager;
import org.sensorhub.impl.module.ModuleRegistry;


/**
 * <p>
 * Default implementation of the processing manager interface
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Feb 28, 2015
 */
public class ProcessingManagerImpl implements IProcessingManager
{
    protected ModuleRegistry moduleRegistry;
    
    
    public ProcessingManagerImpl(ModuleRegistry moduleRegistry)
    {
        this.moduleRegistry = moduleRegistry;
    }
    
    
    @Override
    public List<IProcessModule<?>> getLoadedModules()
    {
        List<IProcessModule<?>> enabledProcesses = new ArrayList<IProcessModule<?>>();
        
        // retrieve all modules implementing IProcessModule
        for (IModule<?> module: moduleRegistry.getLoadedModules())
        {
            if (module instanceof IProcessModule)
                enabledProcesses.add((IProcessModule<?>)module);
        }
        
        return enabledProcesses;
    }
    
    
    @Override
    public boolean isModuleLoaded(String moduleID)
    {
        return moduleRegistry.isModuleLoaded(moduleID);
    }


    @Override
    public List<ModuleConfig> getAvailableModules()
    {
        List<ModuleConfig> configuredProcesses = new ArrayList<ModuleConfig>();
        
        // retrieve all modules implementing ISensorInterface
        for (ModuleConfig config: moduleRegistry.getAvailableModules())
        {
            try
            {
                if (IProcessModule.class.isAssignableFrom(Class.forName(config.moduleClass)))
                    configuredProcesses.add(config);
            }
            catch (Exception e)
            {
            }
        }
        
        return configuredProcesses;
    }


    @Override
    public IProcessModule<?> getModuleById(String moduleID) throws SensorHubException
    {
        IModule<?> module = moduleRegistry.getModuleById(moduleID);
        
        if (module instanceof IProcessModule<?>)
            return (IProcessModule<?>)module;
        else
            return null;
    }


    @Override
    public List<String> getAllProcessCodePackages()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void installProcessCode(String processURI, URL codePackage, boolean replace)
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void uninstallProcessCode(String processURI)
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void syncExec(String processID)
    {
        // TODO Auto-generated method stub
        
    }


    @Override
    public Future<?> asyncExec(String processID, int priority)
    {
        // TODO Auto-generated method stub
        return null;
    }
    
}
