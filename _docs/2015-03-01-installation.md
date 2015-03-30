---
layout: page
title:  "Install SensorHub"
date:   2015-03-01 10:40:56
categories: 1.0 tutorial
---

This page describes how to install SensorHub v1.0-beta binary release so you can test it on your own platform. The process is actually really easy thanks to the use of embedded Jetty, so it should not take you more than 5 minutes to get a running SensorHub instance on your machine.

### Setup

  * First download the v1.0-beta release from [here](https://github.com/sensiasoft/sensorhub/releases)
  * Unzip the file to a directory of your choice
  * Execute the `launch.sh` script (on Linux)
  * You should now be able to connect to `http://localhost:8080/sensorhub/test` and get the message `SensorHub web server is up`
  
*Note: This release has been tested on various Linux platforms as well as MacOS but much less on Windows.*


### Demo Configuration

The demo configuration provided with the binary release instructs SensorHub to start the following components:

  * The embedded Jetty server
  * The web admin UI
  * The fake GPS example sensor
  * The fake weather example sensor
  * A simple linear transform stream process
  * Embedded storage instances for data produced by the 2 sensors and process
  * An SOS service connected to all 3 real-time feeds and storages


### Connect to the Sensor Observation Service (SOS)

You can connect to the SOS right away, to get sensor data and metadata:

  * GetCapabilities: <http://localhost:8080/sensorhub/sos?service=SOS&version=2.0&request=GetCapabilities>
  * Get Weather Result Template: <http://localhost:8080/sensorhub/sos?service=SOS&version=2.0&request=GetResultTemplate&offering=urn:mysos:offering03&observedProperty=http://sensorml.com/ont/swe/property/Weather>
  * Get Latest Weather Measurement: <http://localhost:8080/sensorhub/sos?service=SOS&version=2.0&request=GetResult&offering=urn:mysos:offering03&observedProperty=http://sensorml.com/ont/swe/property/Weather&temporalFilter=phenomenonTime,now>
  * Get Historical Weather Measurements: <http://localhost:8080/sensorhub/sos?service=SOS&version=2.0&request=GetResult&offering=urn:mysos:offering03&observedProperty=http://sensorml.com/ont/swe/property/Weather&temporalFilter=phenomenonTime,2015-01-01/now>
  
Also take a look at this simple [demo client](http://sensiasoft.net:8181/osm_client_websockets.html) that connects to the fake GPS live feed through websockets to display it on a map using OpenLayers. You can easily reproduce this locally.


### Connect to the Admin console (still in pre-alpha stage)

You can connect to the admin console at [http://localhost:8080/sensorhub/admin](http://localhost:8080/sensorhub/admin).

Please bear with us as this is still at very early stage. Even though this component is not as mature as the rest of the software, we still wanted to show it to you
