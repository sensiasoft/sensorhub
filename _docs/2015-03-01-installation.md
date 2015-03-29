---
layout: page
title:  "Install SensorHub"
date:   2015-03-01 10:40:56
categories: 1.0 tutorial
---

This page describes how to install SensorHub v1.0-beta binary release so you can test it on your own platform. The process is actually really easy thanks the to the use of embedded Jetty, so it should not take you more than 5 minutes to get a running SensorHub instance on your machine.

  * First download the v1.0-beta release from [here](https://github.com/sensiasoft/sensorhub/releases)
  * Unzip the file to a directory of your choice
  * Execute the `launch.sh` script (on Linux)
  * You should now be able to connect to `http://localhost:8080/sensorhub/test` and get the message `SensorHub web server is up`
  
*Note: This release has been tested on various Linux platform and MacOS but much less on Windows, although it seemed to basically work last time I checked.*

