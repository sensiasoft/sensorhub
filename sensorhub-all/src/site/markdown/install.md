How To Install SensorHub
---

This page describes how to install SensorHub binary release so you can test it on your own platform. The process is actually really easy thanks to the use of embedded Jetty, so it should not take you more than 5 minutes to get a running SensorHub instance on your machine. (_NOTE: Installation on Android phones and tablets is through a separate APK file_).


### Setup

  * First download the latest SensorHub binary release from [Here](https://github.com/sensiasoft/sensorhub/releases)
  * Unzip the file to a directory of your choice
  * Execute the `launch.sh` script (on Linux)
  * You should now be able to connect to <http://localhost:8080/sensorhub/test> and get the message `SensorHub web server is up`
  
*Note: This release has been tested on various Linux platforms as well as MacOS but much less on Windows.*


### Demo Configuration

The demo configuration provided with the binary release instructs SensorHub to start the following components:

  * The embedded Jetty server
  * The web admin UI
  * The simulated GPS example sensor
  * The simulated weather example sensor
  * Embedded storage instances for data produced by the 2 sensors
  * An SOS service connected to the real-time feeds and storages


### Connect to the Sensor Observation Service (SOS)

You can connect to the SOS deployed at <http://localhost:8080/sensorhub/sos> right away, to get sensor data and metadata:

  * [GetCapabilities](http://localhost:8080/sensorhub/sos?service=SOS&amp;version=2.0&amp;request=GetCapabilities)
  * [Get Weather Result Template](http://localhost:8080/sensorhub/sos?service=SOS&amp;version=2.0&amp;request=GetResultTemplate&amp;offering=urn:mysos:offering03&amp;observedProperty=http://sensorml.com/ont/swe/property/Weather)
  * [Get Latest Weather Measurement](http://localhost:8080/sensorhub/sos?service=SOS&amp;version=2.0&amp;request=GetResult&amp;offering=urn:mysos:offering03&amp;observedProperty=http://sensorml.com/ont/swe/property/Weather&amp;temporalFilter=phenomenonTime,now)
  * [Get Historical Weather Measurements](http://localhost:8080/sensorhub/sos?service=SOS&amp;version=2.0&amp;request=GetResult&amp;offering=urn:mysos:offering03&amp;observedProperty=http://sensorml.com/ont/swe/property/Weather&amp;temporalFilter=phenomenonTime,2015-01-01/now)
  
Also take a look at this simple [demo client](http://sensiasoft.net:8181/osm_client_websockets.html) that connects to the fake GPS live feed through websockets to display it on a map using OpenLayers. You can easily reproduce this locally.


### Connect to the Admin Console

You can connect to the [Admin Console](images/webui1.png "SensorHub Admin Web UI") at <http://localhost:8080/sensorhub/admin>.

When active, the console allows you to manage all SensorHub modules including sensors, processing chains, storage units, as well as service interfaces such as Sensor Observation Services (SOS) or Sensor Planning Services (SPS).

