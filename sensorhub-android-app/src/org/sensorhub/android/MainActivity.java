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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import org.sensorhub.android.SOSTClient.StreamInfo;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.impl.module.InMemoryConfigDb;
import org.sensorhub.impl.sensor.android.AndroidSensorsConfig;
import org.sensorhub.impl.sensor.android.AndroidSensorsDriver;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class MainActivity extends Activity
{
    TextView textArea;
    SensorHubService boundService;
    IModuleConfigRepository sensorhubConfig;
    Timer statusTimer;
    Handler displayHandler;
    StringBuilder displayText = new StringBuilder();
    
    
    private ServiceConnection sConn = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            boundService = ((SensorHubService.LocalBinder)service).getService();
            boundService.startSensorHub(sensorhubConfig);
            startStatusDisplay();
        }

        public void onServiceDisconnected(ComponentName className)
        {
            boundService = null;
        }
    };

    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textArea = (TextView)findViewById(R.id.text);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        /*SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);*/
        
        // set config
        sensorhubConfig = new InMemoryConfigDb();
        AndroidSensorsConfig sensorsConfig = new AndroidSensorsConfig();
        sensorsConfig.id = "ANDROID_SENSORS";
        sensorsConfig.name = "Android Sensors";
        sensorsConfig.enabled = true;
        sensorsConfig.moduleClass = AndroidSensorsDriver.class.getCanonicalName();
        sensorsConfig.activateAccelerometer = prefs.getBoolean("accel_enabled", false);
        sensorsConfig.activateGyrometer = prefs.getBoolean("gyro_enabled", false);
        sensorsConfig.activateMagnetometer = prefs.getBoolean("mag_enabled", false);
        sensorsConfig.activateOrientation = prefs.getBoolean("orient_enabled", false);
        sensorsConfig.activateGpsLocation = prefs.getBoolean("gps_enabled", false);
        sensorsConfig.activateNetworkLocation = prefs.getBoolean("netloc_enabled", false);
        sensorsConfig.activateBackCamera = prefs.getBoolean("cam_enabled", false);
        sensorsConfig.sosEndpoint = prefs.getString("sos_uri", "");
        AndroidSensorsDriver.androidContext = this.getApplicationContext();
        sensorhubConfig.add(sensorsConfig);
        
        // start SensorHub service
        Context context = this.getApplicationContext();
        Intent intent = new Intent(context, SensorHubService.class);
        //context.startService(intent);
        context.bindService(intent, sConn, Context.BIND_AUTO_CREATE);
        
        displayHandler = new Handler() {
            public void handleMessage(Message msg) {
                textArea.setText(Html.fromHtml(displayText.toString()));
            }
        };
    }
    
    
    protected void startStatusDisplay()
    {
        statusTimer = new Timer();
        statusTimer.scheduleAtFixedRate(new TimerTask() {
            public void run()
            {
                displayText.setLength(0);
                
                if (boundService == null || boundService.getSosClient() == null)
                {
                    displayText.append("Waiting for SensorHub service to start...");
                }
                else if (!boundService.getSosClient().isConnected())
                {
                    displayText.append("<font color='red'><b>Cannot connect to SOS-T</b></font><br/>");
                    displayText.append("Please check your settings...");
                }
                else
                { 
                    Map<ISensorDataInterface, StreamInfo> dataStreams = boundService.getSosClient().getDataStreams();
                    long now = System.currentTimeMillis();
                    for (Entry<ISensorDataInterface, StreamInfo> stream: dataStreams.entrySet())
                    {
                        displayText.append("<p><b>" + stream.getKey().getName() + " : </b>");
                        if (now - stream.getValue().lastSampleTime > 2000)
                            displayText.append("<font color='red'>NOK</font>");
                        else
                            displayText.append("<font color='green'>OK</font>");
                        displayText.append("</p>");
                    }
                }
               
                displayHandler.obtainMessage(1).sendToTarget();
            }
        }, 0, 1000L);
    }
    
    
    protected void stopStatusDisplay()
    {
        if (statusTimer != null)
            statusTimer.cancel();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings)
        {
            startActivity(new Intent(this, UserSettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume()
    {
        startStatusDisplay();
        super.onResume();
    }


    @Override
    protected void onPause()
    {
        stopStatusDisplay();
        super.onPause();
    }


    @Override
    protected void onDestroy()
    {
        Context context = this.getApplicationContext();
        context.stopService(new Intent(context, SensorHubService.class));
        super.onDestroy();
    }
}
