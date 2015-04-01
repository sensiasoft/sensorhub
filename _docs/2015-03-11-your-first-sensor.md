---
layout: page
title:  "Your First Sensor Driver"
date:   2015-03-11 10:40:56
categories: 1.0 tutorial
---


This is a tutorial to help you write your first sensor driver, based on the [Fake Weather][] demo module that is provided with SensorHub source and binary releases.

The first step is to create the maven project that will contain the new sensor module. Follow the [steps on the wiki](https://github.com/sensiasoft/sensorhub/wiki/Adding-new-modules) to create a new Eclipse Maven project. For the sake of coherency, you should name your driver project `sensorhub-driver-{your_driver_name}`. In the case of the Fake Weather module, we named it `sensorhub-driver-fakeweather`.

You need to create at least 4 classes to add a new sensor module to the SensorHub system:

  * The module configuration class
  * The main sensor module class
  * At least one sensor output class
  * The module descriptor class

[Fake Weather]: https://github.com/sensiasoft/sensorhub/tree/master/sensorhub-driver-fakeweather/src/main/java/org/sensorhub/impl/sensor/fakeweather


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

[FakeWeatherConfig]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-driver-fakeweather/src/main/java/org/sensorhub/impl/sensor/fakeweather/FakeWeatherConfig.java



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


[ISensorModule]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/sensor/ISensorModule.java

[AbstractSensorModule]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/impl/sensor/AbstractSensorModule.java

[FakeWeatherSensor]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-driver-fakeweather/src/main/java/org/sensorhub/impl/sensor/fakeweather/FakeWeatherSensor.java

[ISensorDataInterface]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/sensor/ISensorDataInterface.java


### The Sensor Output Class

Each output interface of a sensor must be defined by a class implementing [ISensorDataInterface][]. Just like for the main sensor module class, we provide the [AbstractSensorOutput][] base class that already implements functionalities common to most sensors, so we highly recommend that you derive from it. For instance, the sole output of the Fake Weather example sensor is defined in the [FakeWeatherOutput][] class.

The main functions of the sensor output class are to:

  * Define the output the output data structure and encoding
  
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
  * Provide the approximate/average sampling time of this output
  * Start/stop measurement thread that gets readings from sensor hardware and package them in a [DataBlock][]
  * Provide access to the latest measurement record and corresponding time stamp




[FakeWeatherOutput]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-driver-fakeweather/src/main/java/org/sensorhub/impl/sensor/fakeweather/FakeWeatherOutput.java

[AbstractSensorOutput]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/impl/sensor/AbstractSensorOutput.java

[DataBlock]:

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

In order to be discoverable by the [ServiceLoader][] API, the module descriptor class also needs to be advertised in a provider-configuration file called `org.sensorhub.api.module.IModuleProvider` in the resource directory `META-INF/services` (see [ServiceLoader][] documentation on Oracle website). For instance, the Fake Weather sensor module includes [this file](https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-driver-fakeweather/src/main/resources/META-INF/services/org.sensorhub.api.module.IModuleProvider) file with the following line:

```
org.sensorhub.impl.sensor.fakeweather.FakeWeatherModuleDescriptor
```

[SensorConfig]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/sensor/SensorConfig.java

[ServiceLoader]: http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html
