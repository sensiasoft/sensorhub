
package org.sensorhub.android;

import java.util.UUID;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.InMemoryConfigDb;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.android.AndroidSensorsConfig;
import org.sensorhub.impl.sensor.android.AndroidSensorsDriver;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        try
        {
            ModuleRegistry registry = new ModuleRegistry(new InMemoryConfigDb());
            
            AndroidSensorsConfig config = new AndroidSensorsConfig();
            config.id = UUID.randomUUID().toString();
            config.name = "Android Sensors";
            config.enabled = true;
            config.moduleClass = AndroidSensorsDriver.class.getCanonicalName();
            AndroidSensorsDriver.androidContext = this.getApplicationContext();
            
            registry.loadModule(config);
            SensorHub.createInstance(null, registry);
        }
        catch (SensorHubException e)
        {
            Log.e("SensorHub", "Error while starting modules", e);
        }
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
