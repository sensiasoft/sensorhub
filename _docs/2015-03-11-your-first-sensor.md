---
layout: page
title:  "Your first sensor driver"
date:   2015-03-11 10:40:56
categories: 1.0 tutorial
---



This is a tutorial to help you write your first sensor driver, based on the "[fake weather][]" demo module that is provided with SensorHub source and binary releases.

The first step is to create the maven project that will contain the new sensor module. Follow the [steps on the wiki](https://github.com/sensiasoft/sensorhub/wiki/Adding-new-modules) to create a new Eclipse Maven project. For the sake of coherency, you should name your driver project `sensorhub-driver-{your_driver_name}`. In the case of the weather station, we name it `sensorhub-driver-fakeweather`.

You need to create at least 4 classes to add a new sensor module to the SensorHub system:
 * The module configuration class
 * The module descriptor class so that your new module can be automatically discovered
 * The main sensor module class
 * At least one sensor output class

[fake weather]: https://github.com/sensiasoft/sensorhub/tree/master/sensorhub-driver-fakeweather/src/main/java/org/sensorhub/impl/sensor/fakeweather

### The module configuration class

The sensor module configuration class must be derived from [SensorConfig][]. You can add any other properties that your sensor needs to be properly configured. This class will be directly initialized by parsing equivalent JSON properties in the main SensorHub configuration file.

```java

```


[SensorConfig]: https://github.com/sensiasoft/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/sensor/SensorConfig.java
