---
layout: post
title:  "SensorHub v1.0 beta released"
date:   2015-03-31 10:40:56
categories: blog
---

[SensorHub v1.0 Beta][] was released today for testing by early adopters. This version includes support for the following features:

  * Sensor drivers API and examples 
  * Sensor Observation Service
  * Sensor Planning Service (only synchronous commands supported)
  * Persistence using PERST Embedded Database
  * Basic SensorML processing capabilities

The following sensor drivers are also included in this release:

  * Fake GPS Sensor (based on random + realistic vehicle movements obtained Google Driving Directions)
  * Fake WeatherStation
  * V4Linux Cameras
  * Axis Camera (with PTZ control)
  * Android Sensors (motion sensors, GPS location, cell/wifi location, and cameras)
  
It has been tested on Linux x86 and ARM with OpenJDK 7 and Oracle JDK 7, MacOS and Windows XP. 

[SensorHub v1.0 Beta]: https://github.com/sensiasoft/sensorhub/releases/tag/v1.0-beta
