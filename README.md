SensorHub
=========

SensorHub software allows one to easily build interoperable and evolutive sensor networks with advanced processing capabilities and based on open-standards for all data exchanges. These open-standards are mostly OGC standards from the Sensor Web Enablement (SWE) initiative and are key to design sensor networks that can largely evolve with time (addition of new types of sensors, reconfigurations, etc.).

The framework allows one to connect any kind of sensors and actuators to a common bus via a simple yet generic driver API. Sensors can be connected through any available hardware interface such as RS232/422, SPI, I2C, USB, Ethernet, Wifi, Bluetooth, ZigBee, HTTP, etc... Once drivers are available for a specific sensor, it is automatically connected to the bus and it is then trivial to send commands and read data from it. An intuitive user interface allows the user to configure the network to suit its needs and more advanced processing capabilities are available via a plugin system.

SensorHub embeds the full power of OGC web services ([Sensor Observation Service] or SOS, Sensor Planning Service or SPS) to communicate with all connected sensors in the network as well as to provide robust metadata (owner, location and orientation, calibration, etc.) about them. Through these standard, several SensorHub instance can also communicate with each other to form larger networks.

Low level functions of SensorHub (send commands and read data from sensor) are coded efficiently and can be used on embedded hardware running Java SE®, Java ME® or Android® while more advanced data processing capabilities are fully multi-threaded and can thus benefit from a more powerful hardware platform (e.g. multi-processor servers or even clusters).

Please take a look at the User's Guide wiki page and the Developer's Guide if you're planning to tweak the code or contribute to the project.
