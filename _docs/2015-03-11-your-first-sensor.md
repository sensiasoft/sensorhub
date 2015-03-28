---
layout: page
title:  "Your first sensor driver"
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


### The module configuration class

The sensor module configuration class must be derived from [SensorConfig][]. You can add any other properties that your sensor needs to be properly configured. This class will be directly initialized by parsing equivalent JSON properties in the main SensorHub configuration file.

In the Fake Weather module, we simply added configuration fields to specify the station location:

```java
public class FakeWeatherConfig extends SensorConfig
{
  public double centerLatitude = 34.8038; // in deg
  public double centerLongitude = -86.7228; // in deg
  public double centerAltitude = 0.000; // in meters
}
```

Note that you can add annotations to provide hints UI to render teh fields nicely. This is shown below:


### The main module class



### The sensor output class



### The module descriptor class

A module descriptor class must be provided to enable automatic discovery of your new module by the SensorHub module registry. By providing a class implementing the `IModuleProvider` interface, all SensorHub modules available on the classpath can indeed be discovered using the standard Java ServiceLoader API.

The class provides metadata about the module such as a name, description and version. It also indicates which configuration class and module class implements the module. It should thus point to the classes you created in the first two steps of this tutorial.

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
