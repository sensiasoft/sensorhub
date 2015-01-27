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

package org.sensorhub.android;

import javax.xml.parsers.DocumentBuilderFactory;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.android.AndroidSensorsDriver;
import org.vast.xml.XMLImplFinder;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;


/**
 * <p>
 * Android Service wrapping the sensorhub instance
 * </p>
 *
 * <p>Copyright (c) 2014 Sensia Software LLC</p>
 * @author Alexandre Robin <alex.robin@sensiasoftware.com>
 * @since Jan 24, 2015
 */
public class SensorHubService extends Service
{
    final IBinder binder = new LocalBinder();
    private Thread bgThread;
    Looper bgLooper;
    SOSTClient sosClient;
    
    
    public class LocalBinder extends Binder {
        SensorHubService getService() {
            return SensorHubService.this;
        }
    }

        
    @Override
    public void onCreate() {
        
        try
        {
            // load external dex file containing stax API
            Dexter.loadFromAssets(this.getApplicationContext(), "stax-api-1.0-2.dex");
            
            // set default StAX implementation
            XMLImplFinder.setStaxInputFactory(com.ctc.wstx.stax.WstxInputFactory.class.newInstance());
            XMLImplFinder.setStaxOutputFactory(com.ctc.wstx.stax.WstxOutputFactory.class.newInstance());
            
            // set default DOM implementation
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            XMLImplFinder.setDOMImplementation(dbf.newDocumentBuilder().getDOMImplementation());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }        
    }
    
    
    public void startSensorHub(final IModuleConfigRepository config)
    {
        if (bgThread == null)
        {
            bgThread = new Thread() {
                
                public void run() 
                {
                    // prepare for processing sensor messages
                    Looper.prepare();
                    bgLooper = Looper.myLooper();
                    
                    // start sensorhub
                    SensorHub.createInstance(null, new ModuleRegistry(config)).start();
                    Log.i("SensorHub", "SensorHub started...");
                    
                    // connect to remote SOS
                    AndroidSensorsDriver sensor = (AndroidSensorsDriver)SensorHub.getInstance().getModuleRegistry().getLoadedModules().get(0); 
                    sosClient = new SOSTClient(sensor.getConfiguration().sosEndpoint);
                    try
                    {
                        // register sensor
                        sosClient.registerSensor(sensor);
                        Log.i("SensorHub", "Android device registered with SOS");
                        
                        // register all templates and start streaming data
                        for (ISensorDataInterface o: sensor.getAllOutputs().values())
                            sosClient.registerDataStream(o);
                        Log.i("SensorHub", "Result templates registered with SOS");
                    }
                    catch (Exception e)
                    {
                        String msg = "Error while registering device with remote SOS";
                        Log.e("SensorHub", msg, e);
                        return;
                    }                    

                    Looper.loop();
                }        
            };
            
            bgThread.start();
        }
    }
    
    
    public void stopSensorHub()
    {
        if (sosClient != null)
        {
            sosClient.stop();
            sosClient = null;
        }
        
        SensorHub sensorhub = SensorHub.getInstance();
        if (sensorhub != null)
        {
            sensorhub.stop();
            Log.i("SensorHub", "SensorHub stopped...");
        }
        
        bgLooper.quit();
        bgLooper = null;
        bgThread = null;
    }    
    

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {        
        return START_STICKY;
    }
    

    @Override
    public void onDestroy() {
        stopSensorHub();
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }


    public SOSTClient getSosClient()
    {
        return sosClient;
    }

}
