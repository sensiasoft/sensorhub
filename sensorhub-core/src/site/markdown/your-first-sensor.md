Your First Sensor Driver
---


This is a tutorial to help you write your first sensor driver, based on the [Fake Weather][] demo module that is provided with SensorHub source and binary releases. You may also find it easier to follow these steps in a [presentation](https://drive.google.com/open?id=0B3EZQJqOfG9sUFlPMkRZZ056VGc) with screenshots that we made during a FOSS4G workshop in July 2015.

[Fake Weather]: https://github.com/opensensorhub/osh-sensors/tree/master/sensorhub-driver-fakeweather/src/main/java/org/sensorhub/impl/sensor/fakeweather


### Maven Project

The first step is to create a new Eclipse Maven project that will contain the new sensor module, as instructed on [this page](adding-new-modules.html). For the sake of coherency, you should name your driver project `sensorhub-driver-{your_driver_name}`. In the case of the Fake Weather module, we named it `sensorhub-driver-fakeweather`.

You then need to create at least 4 classes to add a new sensor module to the SensorHub system:

  * The module configuration class
  * The main sensor module class
  * At least one sensor output class
  * The module descriptor class


### The Module Configuration Class

The sensor module configuration class must be derived from [SensorConfig][]. You can add any other properties that your sensor needs to be properly configured. This class will be directly initialized by parsing equivalent JSON properties in the main SensorHub configuration file.

The configuration class for the Fake Weather module is [FakeWeatherConfig][], where we simply added fields to specify the station location:

```java
public class FakeWeatherConfig extends SensorConfig
{
  public double centerLatitude = 34.8038; // in deg
  public double centerLongitude = -86.7228; // in deg
  public double centerAltitude = 0.000; // in meters
}
```

We recommend that you use the `@DisplayInfo` annotation to provide rendering hints for UI classes. An example of this is shown below:

```java
public class FakeWeatherConfig extends SensorConfig
{
  @DisplayInfo(label="Latitude", desc="Latitude of Weather Station")
  public double centerLatitude = 34.8038; // in deg
  
  @DisplayInfo(label="Longitude", desc="Longitude of Weather Station")
  public double centerLongitude = -86.7228; // in deg
  
  @DisplayInfo(label="Altitude", desc="Altitude of Weather Station")
  public double centerAltitude = 150.000; // in meters
}
```

Below is a JSON snippet to be included in the main SensorHub configuration file, giving a possible configuration for the Fake Weather module:

```json
{
  "objClass": "org.sensorhub.impl.sensor.fakeweather.FakeWeatherConfig",
  "id": "d136b6ea-3950-4691-bf56-c84ec7d89d73",
  "name": "Fake Weather Sensor",
  "enabled": true,
  "moduleClass": "org.sensorhub.impl.sensor.fakeweather.FakeWeatherSensor",
  "sensorML": null,
  "autoActivate": true,
  "enableHistory": false,
  "hiddenIO": null,
  "centerLatitude": 43.6182,
  "centerLongitude": 1.4238,
  "centerAltitude": 150.0
}
```

[FakeWeatherConfig]: https://github.com/opensensorhub/osh-sensors/blob/master/sensorhub-driver-fakeweather/src/main/java/org/sensorhub/impl/sensor/fakeweather/FakeWeatherConfig.java



### The Sensor Module Class

The sensor module class is the main entry point to the sensor driver implementation and must implement the generic [ISensorModule][] interface. You can implement this interface directly but in most cases you should derive from the [AbstractSensorModule][] class instead since it already provides some functionality common to most sensors. In both cases, your must further specify your class by setting the configuration class that you defined at the previous step as its generic parameter. 

This is shown below for the Fake Weather example:

```java
public class FakeWeatherSensor extends AbstractSensorModule<FakeWeatherConfig>
```

The sensor module class is responsible for creating an output interface object (implementation of [ISensorDataInterface][]) for each sensor ouput and preparing the SensorML description of the sensor.

For the Fake Weather example module, implementation is provided in [FakeWeatherSensor][]. This module only defines a single output and no control input. The next snippet shows the constructor where the output interface is instantiated, initialized, and appended to the output list using the `addOutput()` method provided by [AbstractSensorModule][]:

```java
public FakeWeatherSensor()
{
  dataInterface = new FakeWeatherOutput(this);
  addOutput(dataInterface, false);
  dataInterface.init();
}
```

The module `start()` and `stop()` methods must also be implemented. They must do all processing needed when the sensor is enabled or disabled respectively. In the case of the Fake Weather module, these methods simply delegate to the output interface since it is this class that actually starts/stops the measurement thread.

```java
public void start() throws SensorHubException
{
    dataInterface.start();        
}

public void stop() throws SensorHubException
{
    dataInterface.stop();
}
```

[ISensorModule]: https://github.com/opensensorhub/osh-core/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/sensor/ISensorModule.java

[AbstractSensorModule]: https://github.com/opensensorhub/osh-core/blob/master/sensorhub-core/src/main/java/org/sensorhub/impl/sensor/AbstractSensorModule.java

[FakeWeatherSensor]: https://github.com/opensensorhub/osh-sensors/blob/master/sensorhub-driver-fakeweather/src/main/java/org/sensorhub/impl/sensor/fakeweather/FakeWeatherSensor.java

[ISensorDataInterface]: https://github.com/opensensorhub/osh-core/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/sensor/ISensorDataInterface.java



### The Sensor Output Class

Each output interface of a sensor must be defined by a class implementing [ISensorDataInterface][]. Just like for the main sensor module class, we provide the [AbstractSensorOutput][] base class that already implements functionalities common to most sensors, so we highly recommend that you derive from it. For instance, the sole output of the Fake Weather example sensor is defined in the [FakeWeatherOutput][] class.

The main functions of the sensor output class are to:

* Define the output data structure and encoding
* Provide the approximate/average sampling time of this output
* Start/stop measurement collection thread
* Provide access to the latest measurement record and corresponding time stamp
 
#### Define the output data structure and encoding

The snippet below is extracted from the Fake Weather example and shows how to create the measurement record structure using the [SWEHelper][] class:

```java
protected void init()
{
    SWEHelper fac = new SWEHelper();
    
    // build SWE Common record structure
    weatherData = new DataRecordImpl(5);
    weatherData.setName(getName());
    weatherData.setDefinition("http://sensorml.com/ont/swe/property/Weather");
    
    // add time, temperature, pressure, wind speed and wind direction fields
    weatherData.addComponent("time", fac.newTimeStampIsoUTC());
    weatherData.addComponent("temperature", fac.newQuantity(SWEHelper.getPropertyUri("AirTemperature"), "Air Temperature", null, "Cel"));
    weatherData.addComponent("pressure", fac.newQuantity(SWEHelper.getPropertyUri("AtmosphericPressure"), "Air Pressure", null, "hPa"));
    weatherData.addComponent("windSpeed", fac.newQuantity(SWEHelper.getPropertyUri("WindSpeed"), "Wind Speed", null, "m/s"));
    
    // for wind direction, we also specify a reference frame
    Quantity q = fac.newQuantity(SWEHelper.getPropertyUri("WindDirection"), "Wind Direction", null, "deg");
    q.setReferenceFrame("http://sensorml.com/ont/swe/property/NED");
    q.setAxisID("z");
    weatherData.addComponent("windDirection", q);
    
    // also generate encoding definition
    weatherEncoding = fac.newTextEncoding(",", "\n");
}
```

In this case, the sensor output is a record composed of the following values:

  * Time stamp (ISO time stamp expressed in UTC time frame)
  * Air temperature
  * Atmospheric pressure
  * Wind speed
  * Wind direction (rotation about Z axis of NED frame)

[SWEHelper]: https://github.com/sensiasoft/lib-swe-common/blob/master/swe-common-core/src/main/java/org/vast/swe/SWEHelper.java


#### Provide the approximate/average sampling time of this output

This is achieved by implementing a simple method that must return the approximative sampling period (in seconds) of data generated by this output. If the rate is known and quasi constant, the method simply returns a fixed value, as in the Fake Weather example:

```java
public double getAverageSamplingPeriod()
{
    // sample every 1 second
    return 1.0;
}
```

When the rate is not known a-priori and/or can vary, an average can be computed online (This is what is done in the [SOS-T Virtual Sensor][] for example since there is no way to know the rate of incoming data in advance).

[SOS-T Virtual Sensor]: https://github.com/opensensorhub/osh-core/blob/master/sensorhub-service-swe/src/main/java/org/sensorhub/impl/sensor/sost/SOSVirtualSensorOutput.java


#### Start/stop measurement collection thread

The measurement thread gets readings from sensor hardware and package them in a [DataBlock][]. The sensor output must thus provide methods to start/stop the measurement thread and implement the logic for connecting to the sensor and correctly generating the [DataBlock][].

In the Fake Weather example, the `sendMeasurement()` method is implemented and is called regularly using a Timer thread set to execute at the frequency specified by `getAverageSamplingPeriod()`.

```java
private void sendMeasurement()
{
    // generate new weather values
    double time = System.currentTimeMillis() / 1000.;
    
    // temperature; value will increase or decrease by less than 1.0 deg
    temp += 0.005 * (2.0 *Math.random() - 1.0);
    
    // pressure; value will increase or decrease by less than 20 hPa
    pressure += 20. * (2.0 * Math.random() - 1.0);
    
    // wind speed; keep positive
    // vary value between +/- 10 m/s
    speed += 10.0 * (2.0 * Math.random() - 1.0);
    speed = speed < 0.0 ? 0.0 : speed;
    
    // wind direction; keep between 0 and 360 degrees
    direction += 4.0 * (2.0 * Math.random() - 1.0);
    direction = direction < 0.0 ? direction+360.0 : direction;
    direction = direction > 360.0 ? direction-360.0 : direction;
    
    // build and publish datablock
    DataBlock dataBlock = weatherData.createDataBlock();
    dataBlock.setDoubleValue(0, time);
    dataBlock.setDoubleValue(1, temp);
    dataBlock.setDoubleValue(2, pressure);
    dataBlock.setDoubleValue(3, speed);
    dataBlock.setDoubleValue(4, direction);
    
    // update latest record and send event
    latestRecord = dataBlock;
    eventHandler.publishEvent(new SensorDataEvent(time, FakeWeatherOutput.this, dataBlock));
}

protected void start()
{
    if (timer != null)
       return;
    timer = new Timer();
    
    // start main measurement generation thread
    TimerTask task = new TimerTask() {
       public void run()
       {
          sendMeasurement();
       }
    };
    timer.scheduleAtFixedRate(task, 0, (long)(getAverageSamplingPeriod()*1000));
}

protected void stop()
{
    if (timer != null)
    {
        timer.cancel();
        timer = null;
    }
}
```


[FakeWeatherOutput]: https://github.com/opensensorhub/osh-sensors/blob/master/sensorhub-driver-fakeweather/src/main/java/org/sensorhub/impl/sensor/fakeweather/FakeWeatherOutput.java

[AbstractSensorOutput]: https://github.com/opensensorhub/osh-core/blob/master/sensorhub-core/src/main/java/org/sensorhub/impl/sensor/AbstractSensorOutput.java

[DataBlock]: https://github.com/sensiasoft/lib-swe-common/blob/master/swe-common-core/src/main/java/net/opengis/swe/v20/DataBlock.java



### The Module Descriptor Class

A module descriptor class must be provided to enable automatic discovery of your new module by the SensorHub module registry. By providing a class implementing the `IModuleProvider` interface, all SensorHub modules available on the classpath can indeed be discovered using the standard Java [ServiceLoader][] API.

The class provides metadata about the module such as a name, description and version. It also indicates which configuration class and module class make up the module. It should thus point to the classes you created in the first two steps of this tutorial.

The snippet below shows the module descriptor for the Fake Weather sensor module:

```java
public class FakeWeatherModuleDescriptor implements IModuleProvider
{
  @Override
  public String getModuleName()
  {
    return "Fake Weather Sensor";
  }

  @Override
  public String getModuleDescription()
  {
    return "Fake weather station with randomly changing measurements";
  }

  @Override
  public String getModuleVersion()
  {
    return "0.1";
  }

  @Override
  public String getProviderName()
  {
    return "Botts Innovative Research Inc";
  }

  @Override
  public Class<? extends IModule<?>> getModuleClass()
  {
    return FakeWeatherSensor.class;
  }

  @Override
  public Class<? extends ModuleConfig> getModuleConfigClass()
  {
    return FakeWeatherConfig.class;
  }
}
```

In order to be discoverable by the [ServiceLoader][] API, the module descriptor class also needs to be advertised in a provider-configuration file called `org.sensorhub.api.module.IModuleProvider` in the resource directory `META-INF/services` (see [ServiceLoader][] documentation on Oracle website). For instance, the Fake Weather sensor module includes [this file](https://github.com/opensensorhub/osh-sensors/blob/master/sensorhub-driver-fakeweather/src/main/resources/META-INF/services/org.sensorhub.api.module.IModuleProvider) file with the following line:

```
org.sensorhub.impl.sensor.fakeweather.FakeWeatherModuleDescriptor
```

[SensorConfig]: https://github.com/opensensorhub/osh-core/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/sensor/SensorConfig.java

[ServiceLoader]: http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html
