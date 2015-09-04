
Thanks for testing OpenSensorHub (OSH). This installation package includes only basic modules and can
be run out-of-the-box with the default configuration provided in config.json. This configuration
includes two simulated sensors: GPS and weather. It also includes corresponding pre-configured
storage isntances and an SOS instance providing both live and historical data from these two sensors.

WARNING: The simulated GPS sensor needs a working Internet connection to function properly since it
uses Google Direction API to generate a realistic trajectory.

For more sensor drivers, please also download the "sensors" package and copy all jars in the lib
folder.


STARTUP
-------

To start using OSH with the default configuration, just run the command:

>> ./launch.sh


If you want to run it on a server through SSH and keep the process running when you log-out, use

>> nohup ./launch &


ADMIN
-----

After launching OSH, you can connect to the admin console:
http://localhost:8181/sensorhub/admin

or view the SOS server capabilities at :
http://localhost:8181/sensorhub/sos?service=SOS&version=2.0&request=GetCapabilities


TEST WEB CLIENTS
----------------
You can also try the included javascript clients connecting to the SOS server.
These are very simple web pages intended to provide examples of how to efficiently render the data
streamed by the SOS server.

http://localhost:8181/osm_client_websockets.html