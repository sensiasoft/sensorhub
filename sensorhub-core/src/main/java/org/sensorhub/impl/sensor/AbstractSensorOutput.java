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

package org.sensorhub.impl.sensor;

import java.util.List;
import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.common.IEventHandler;
import org.sensorhub.api.common.IEventListener;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorException;
import org.sensorhub.impl.common.BasicEventHandler;
import org.sensorhub.utils.MsgUtils;


/**
 * <p>
 * Class providing default implementation of common sensor data interface
 * API methods. By default, push is reported as supported and storage
 * is reported as unsupported.
 * </p>
 *
 * <p>Copyright (c) 2013</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @param <SensorType> Type of parent sensor
 * @since Nov 2, 2014
 */
public abstract class AbstractSensorOutput<SensorType extends ISensorModule<?>> implements ISensorDataInterface
{
    protected static String ERROR_NO_STORAGE = "Data storage is not supported by driver ";
    protected SensorType parentSensor;
    protected IEventHandler eventHandler;
    
    
    public AbstractSensorOutput(SensorType parentSensor)
    {
        this.parentSensor = parentSensor;
        this.eventHandler = new BasicEventHandler();
    }
    
    
    protected abstract void init() throws SensorException;
    
    
    protected void stop()
    {
        // do nothing by default
    }
    
    
    @Override
    public SensorType getParentSensor()
    {
        return parentSensor;
    }


    @Override
    public boolean isEnabled()
    {
        return true;
    }


    @Override
    public boolean isPushSupported()
    {
        return true;
    }


    @Override
    public boolean isStorageSupported()
    {
        return false;
    }


    @Override
    public int getStorageCapacity() throws SensorException
    {
        return 0;
    }


    @Override
    public int getNumberOfAvailableRecords() throws SensorException
    {
        throw new SensorException(ERROR_NO_STORAGE + MsgUtils.moduleClassAndId(parentSensor));
    }


    @Override
    public List<DataBlock> getLatestRecords(int maxRecords, boolean clear) throws SensorException
    {
        throw new SensorException(ERROR_NO_STORAGE + MsgUtils.moduleClassAndId(parentSensor));
    }


    @Override
    public List<DataBlock> getAllRecords(boolean clear) throws SensorException
    {
        throw new SensorException(ERROR_NO_STORAGE + MsgUtils.moduleClassAndId(parentSensor));
    }


    @Override
    public int clearAllRecords() throws SensorException
    {
        throw new SensorException(ERROR_NO_STORAGE + MsgUtils.moduleClassAndId(parentSensor));
    }
    
    
    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }

}
