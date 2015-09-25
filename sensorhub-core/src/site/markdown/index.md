SensorHub Core APIs
---

SensorHub software is implemented on top of public APIs that make the system modular and allow one to change almost any part of the system by pluging in new implementations.


The core APIs are:

- [SWE Common and SensorML Bindings](sensorml-api.html) to create/read/write SensorML documents
- [Sensor API](sensor-api.html) to implement sensor and actuator drivers
- [Process API]() to implement processing chains
- [Persistence API]() to implement bindings to any database system
- [Communication API]() to implement drivers for various communication buses (serial, USB, Bluetooth, I2C, SPI, etc.)
- [Event Manager API]() to implement queuing and dispatching of events between modules or even sensorhub instances 


Before you start implementing SensorHub's modules using these APIs, we also strongly encourage you to take the time to read and understand the key [Design Principles](design-principles.html) used in SensorHub software. 