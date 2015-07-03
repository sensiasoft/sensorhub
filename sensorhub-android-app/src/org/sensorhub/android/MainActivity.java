/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.android;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import org.sensorhub.android.comm.BluetoothCommProvider;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModuleConfigRepository;
import org.sensorhub.api.sensor.ISensorDataInterface;
import org.sensorhub.impl.client.sost.SOSTClient;
import org.sensorhub.impl.client.sost.SOSTClient.StreamInfo;
import org.sensorhub.impl.client.sost.SOSTClientConfig;
import org.sensorhub.impl.comm.BluetoothConfig;
import org.sensorhub.impl.module.InMemoryConfigDb;
import org.sensorhub.impl.sensor.android.AndroidSensorsConfig;
import org.sensorhub.impl.sensor.trupulse.TruPulseConfig;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends Activity implements SurfaceHolder.Callback
{
    private final static String ANDROID_SENSORS_ID = "ANDROID_SENSORS";
    private final static String ANDROID_SENSORS_SOST_ID = "SOST_CLIENT1";
    
    TextView textArea;
    SensorHubService boundService;
    IModuleConfigRepository sensorhubConfig;
    Timer statusTimer;
    Handler displayHandler;
    StringBuilder displayText = new StringBuilder();
    SurfaceHolder camPreviewSurfaceHolder;

    
    private ServiceConnection sConn = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            boundService = ((SensorHubService.LocalBinder) service).getService();
            //boundService.startSensorHub(sensorhubConfig);
            startStatusDisplay();
        }


        public void onServiceDisconnected(ComponentName className)
        {
            boundService = null;
        }
    };


    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textArea = (TextView) findViewById(R.id.text);
        SurfaceView camPreview = (SurfaceView) findViewById(R.id.textureView1);
        camPreview.getHolder().addCallback(this);

        displayHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                textArea.setText(Html.fromHtml(displayText.toString()));
            }
        };
    }


    protected void updateConfig(SharedPreferences prefs, String runName)
    {
        sensorhubConfig = new InMemoryConfigDb();
        
        AndroidSensorsConfig sensorsConfig = new AndroidSensorsConfig();
        sensorsConfig.id = ANDROID_SENSORS_ID;
        String deviceName = prefs.getString("device_name", null);
        if (deviceName == null || deviceName.length() < 2)
            deviceName = "Android";
        sensorsConfig.name = deviceName + " Sensors";
        sensorsConfig.enabled = true;
        sensorsConfig.activateAccelerometer = prefs.getBoolean("accel_enabled", false);
        sensorsConfig.activateGyrometer = prefs.getBoolean("gyro_enabled", false);
        sensorsConfig.activateMagnetometer = prefs.getBoolean("mag_enabled", false);
        sensorsConfig.activateOrientationQuat = prefs.getBoolean("orient_quat_enabled", false);
        sensorsConfig.activateOrientationEuler = prefs.getBoolean("orient_euler_enabled", false);
        sensorsConfig.activateGpsLocation = prefs.getBoolean("gps_enabled", false);
        sensorsConfig.activateNetworkLocation = prefs.getBoolean("netloc_enabled", false);
        sensorsConfig.activateBackCamera = prefs.getBoolean("cam_enabled", false);
        sensorsConfig.androidContext = this.getApplicationContext();
        sensorsConfig.camPreviewSurfaceHolder = this.camPreviewSurfaceHolder;
        sensorsConfig.runName = runName;
        sensorhubConfig.add(sensorsConfig);
        
        SOSTClientConfig sosConfig1 = new SOSTClientConfig();
        sosConfig1.id = ANDROID_SENSORS_SOST_ID;
        sosConfig1.name = "SOS-T Client for Android Sensors";
        sosConfig1.enabled = true;
        sosConfig1.sensorID = sensorsConfig.id;
        sosConfig1.sosEndpointUrl = prefs.getString("sos_uri", "");
        sosConfig1.usePersistentConnection = true;
        sensorhubConfig.add(sosConfig1);
        
        TruPulseConfig trupulseConfig = new TruPulseConfig();
        trupulseConfig.id = "TruPulse";
        trupulseConfig.name = "TruPulse Range Finder";
        trupulseConfig.enabled = true;
        BluetoothConfig btConf = new BluetoothConfig();
        btConf.moduleClass = BluetoothCommProvider.class.getCanonicalName();
        btConf.deviceName = "TP360RB.*";
        trupulseConfig.commSettings = btConf;
        sensorhubConfig.add(trupulseConfig);
        
        SOSTClientConfig sosConfig2 = new SOSTClientConfig();
        sosConfig2.id = "SOST_CLIENT2";
        sosConfig2.name = "SOS-T Client for TruPulse Sensor";
        sosConfig2.enabled = true;
        sosConfig2.sensorID = trupulseConfig.id;
        sosConfig2.sosEndpointUrl = prefs.getString("sos_uri", "");
        sosConfig2.usePersistentConnection = true;
        sensorhubConfig.add(sosConfig2);
    }


    protected SOSTClient getSosClient()
    {
        if (boundService.getSensorHub() == null)
            return null;

        try
        {
            return (SOSTClient) boundService.getSensorHub().getModuleRegistry().getModuleById(ANDROID_SENSORS_SOST_ID);
        }
        catch (SensorHubException e)
        {
            return null;
        }
    }


    protected void startStatusDisplay()
    {
        statusTimer = new Timer();
        statusTimer.scheduleAtFixedRate(new TimerTask()
        {
            public void run()
            {
                try
                {
                    displayText.setLength(0);
                    
                    if (boundService == null || boundService.getSensorHub() == null)
                    {
                        displayText.append("Waiting for SensorHub service to start...");
                    }
                    else if (getSosClient() == null || !getSosClient().isConnected())
                    {
                        displayText.append("<font color='red'><b>Cannot connect to SOS-T</b></font><br/>");
                        displayText.append("Please check your settings...");
                    }
                    else
                    {
                        Map<ISensorDataInterface, StreamInfo> dataStreams = getSosClient().getDataStreams();
                        if (dataStreams.size() > 0)
                            displayText.append("<p>Registered with SOS-T</p><p>");

                        long now = System.currentTimeMillis();
                        for (Entry<ISensorDataInterface, StreamInfo> stream : dataStreams.entrySet())
                        {
                            displayText.append("<b>" + stream.getKey().getName() + " : </b>");

                            if (now - stream.getValue().lastEventTime > 2000)
                                displayText.append("<font color='red'>NOK</font>");
                            else
                                displayText.append("<font color='green'>OK</font>");

                            if (stream.getValue().errorCount > 0)
                            {
                                displayText.append("<font color='red'> (");
                                displayText.append(stream.getValue().errorCount);
                                displayText.append(")</font>");
                            }

                            displayText.append("<br/>");
                        }

                        displayText.append("</p>");
                    }

                    displayHandler.obtainMessage(1).sendToTarget();
                }
                catch (Exception e)
                {
                    // some exceptions happen due to multithreading issues but we don't care
                    // just keep looping
                }
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
        else if (id == R.id.action_start)
        {
            if (boundService != null)
            {
                boundService.stopSensorHub();
                showRunNamePopup();
            }
            return true;
        }
        else if (id == R.id.action_stop)
        {
            if (boundService != null)
                boundService.stopSensorHub();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    
    protected void showRunNamePopup()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Run Name");
        alert.setMessage("Please enter the name for this run");
        
        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.getText().append("Run-");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        input.getText().append(formatter.format(new Date()));
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                String runName = input.getText().toString();
                updateConfig(PreferenceManager.getDefaultSharedPreferences(MainActivity.this), runName);
                boundService.startSensorHub(sensorhubConfig);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
          }
        });

        alert.show();
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


    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        this.camPreviewSurfaceHolder = holder;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        /*SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);*/

        // start SensorHub service
        Context context = this.getApplicationContext();
        Intent intent = new Intent(context, SensorHubService.class);
        //context.startService(intent);
        context.bindService(intent, sConn, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }
}
