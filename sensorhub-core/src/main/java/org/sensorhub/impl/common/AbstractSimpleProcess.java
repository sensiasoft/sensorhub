/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.common;

import org.sensorhub.api.processing.IProcess;
import org.sensorhub.api.processing.ProcessConfig;


/**
 * <p>
 * Base abstract class for simple core processes
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Aug 30, 2013
 */
public abstract class AbstractSimpleProcess implements IProcess
{
    private ProcessConfig config;
    
    
    public AbstractSimpleProcess()
    {
        super();
    }

    
    @Override
    public void init(ProcessConfig config)
    {
        this.config = config;
    }

    
    @Override
    public ProcessConfig getConfiguration()
    {
        return this.config;
    }


    @Override
    public String getName()
    {
        return config.name;
    }


    @Override
    public String getLocalID()
    {
        return config.id;
    } 
}