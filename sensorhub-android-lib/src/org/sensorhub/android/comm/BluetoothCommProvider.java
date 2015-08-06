/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.android.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.sensorhub.api.comm.ICommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.comm.BluetoothConfig;
import org.sensorhub.impl.module.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import android.bluetooth.BluetoothSocket;


/**
 * <p>
 * Communication provider for Bluetooth Sockets (i.e. Serial Port Profile)
 * using the Android API
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Jun 18, 2015
 */
public class BluetoothCommProvider extends AbstractModule<BluetoothConfig> implements ICommProvider<BluetoothConfig>
{
    static final Logger log = LoggerFactory.getLogger(BluetoothCommProvider.class.getSimpleName());
    BluetoothSocket btSocket;
    
    
    public BluetoothCommProvider() 
    {
    }
    
    
    @Override
    public InputStream getInputStream() throws IOException
    {
        return btSocket.getInputStream();
    }


    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return btSocket.getOutputStream();
    }


    @Override
    public void start() throws SensorHubException
    {
        try
        {
            BluetoothManager btManager = new BluetoothManager();
            btSocket = btManager.connectToSerialDevice(config.deviceName);
            btSocket.connect();
            log.info("Connected to Bluetooth SPP device {}", btSocket.getRemoteDevice().getName());
        }
        catch (IOException e)
        {
            throw new SensorHubException("Cannot connect to BT device", e);
        }        
    }


    @Override
    public void stop() throws SensorHubException
    {
        try
        {
            btSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public void cleanup() throws SensorHubException
    {       
    }

}
