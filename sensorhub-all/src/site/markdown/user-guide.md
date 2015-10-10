User's Guide
---

This guide will walk you through basics of using SensorHub. I you haven't installed it on your platform yet, please do so first by following instructions on the [Download](download.html) and [Install](install.html) pages.


### Web-based Admin Interface

The easiest way to use SensorHub is via the web-based interface. However, if something is not available from the UI, you can always edit the configuration file manually (See section [Configuration File](#Configuration_File)). 

When SensorHub is running, you can connect to the following URL to access the administration page:

    http://localhost:8181/sensorhub/admin

This admin page allows you to do the following actions:

  * Add and configure new sensors (when proper driver was previously installed)
  * Add and configure sensor data storage
  * Add and configure SOS and SPS service instances
  * Expose data streams and/or storage through SOS
  * Expose sensor commands through SPS
  

For more details, see the [Web-based Interface Manual](webui-manual.html).



### Example Javascript Clients




### Configuration File

SensorHub's configuration is centralized in a single file. It is in JSON format so it can be easily viewed or modified in any text editor.

This file contains a list of module's configuration that are loaded in order* when starting SensorHub. 





(*) Modules are loaded in order except if a module needs another module to start. In this case, the dependent module is loaded as needed by the calling module. 


### Module State

The internal state of each module is saved in a subfolder of the `modules` folder whose name is the module's local ID. This folder can contain:

  * A `state.txt` file containing a list of key/value pairs corresponding to state properties that the module has saved
  * Zero or more `.dat` files that contain arbitrary data saved by the module
  
  

