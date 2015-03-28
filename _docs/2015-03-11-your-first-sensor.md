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

For the Fake Weather example module, implementation is provided in [FakeWeatherSensor][]. This module only defines a single output and no control input. The next snippet shows the constructor where the output interface is instantiated, initialized, and appended to the output list using the `addOutput()` method provided by [AbstractSensorOutput][]:

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
@Override
public void start() throws SensorHubException
{
    dataInterface.start();        
}

@Override
public void stop() throws SensorHubException
{
    dataInterface.stop();
}
```


[ISensorModule]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/sensor/ISensorModule.java

[AbstractSensorModule]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/impl/sensor/AbstractSensorModule.java

[FakeWeatherSensor]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-driver-fakeweather/src/main/java/org/sensorhub/impl/sensor/fakeweather/FakeWeatherSensor.java

[ISensorDataInterface]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/sensor/ISensorDataInterface.java

[AbstractSensorOutput]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/impl/sensor/AbstractSensorOutput.java

### The Sensor Output Class




### The Module Descriptor Class

A module descriptor class must be provided to enable automatic discovery of your new module by the SensorHub module registry. By providing a class implementing the `IModuleProvider` interface, all SensorHub modules available on the classpath can indeed be discovered using the standard Java ServiceLoader API.

The class provides metadata about the module such as a name, description and version. It also indicates which configuration class and module class make up the module. It should thus point to the classes you created in the first two steps of this tutorial.

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


[SensorConfig]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/sensor/SensorConfig.java
