Sensor Network API
---

This page presents an extension of the base [Sensor API](sensor-api.html) that allows one to implement drivers for sensor networks of any size as a single SensorHub module. This API is used in addition to the base sensor API when one needs to wrap such a sensor network instead of just a single sensor.

In addition to the base sensor API methods, a sensor network module would have to implement the ['IMultiSourceDataProducer'](https://github.com/opensensorhub/osh-core/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/data/IMultiSourceDataProducer.java) interface which allows providing [Sensor Descriptions](#Sensor_Descriptions) and [Features of Interest](#Features_of_Interest) (FOI) for multiple [Entities](#Entities):
 

### Entities

The 'IMultiSourceDataProducer' interface is based on the concept of _Entity_: An entity represents one member of the network. We use the term entity because this interface can also be used for other things than sensor networks, such as process grids.

For sensor networks, each entity usually corresponds to one sensor (or one measurement system such as a weather station) in the network. Although sensors in a network often measure the same quantities, it is not always the case, so SensorHub allows for completely heterogeneous sensors to be part of the same network.

                    
### Sensor Descriptions

The sensor network driver is responsible for providing sensor descriptions for each entity via the `getCurrentDescription(String entityId)` method. This description can contain information specific to that entity such as its location (when fixed) and calibration tables for example.

Since the driver also inherits the original methods from the base [Sensor API](sensor-api.html), the `getCurrentDescription()` method shall be used to provide a description of the network as a whole. This description usually contains the list of sensors that are part of the network as SensorML components (for large networks, this will be preferably done by reference). It is also important to include discovery related information in this description since it is the only one directly referenced by the capabilities document of SWE services.

Changes in the network or in a given entity configuration are notified using a [SensorEvent](https://github.com/opensensorhub/osh-core/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/sensor/SensorEvent.java). In the case of an entity change, use the constructor with the sensorID.

_Note: As for single sensors, the driver is only required to provide the most current sensor descriptions. Maintenance of the history of descriptions is done by storage modules in SensorHub._


### Features of Interest

In a sensor network, there can be as many features of interest (FOI) as there are entities in the network at any given point in time (e.g. for a network of weather station, there is one static sampling point per station). The FOI associated with each entity is provided via the `getCurrentFeatureOfInterest(String entityID)` method.

Since the driver also inherits the original methods from the base [Sensor API](sensor-api.html), the `getCurrentFeatureOfInterest()` method shall be used to provide a FOI for the network as a whole (e.g. The FOI for a river monitoring sensor network is the river itself, while the FOI for each station/entity would be a sampling point at the station location). This corresponds to the sampled feature in the O&M model.

When one of the features of interest changes (as usually happens in networks of mobile sensors), this can be notified using an [FoiEvent](https://github.com/opensensorhub/osh-core/sensorhub/blob/master/sensorhub-core/src/main/java/org/sensorhub/api/data/FoiEvent.java). If the FOI observed by one of the sensor changes (e.g. the sensor was moved to a new location), use the constructor with the entityID.

_Note: As for single sensors, the driver is only required to provide the feature of interest currently being observed. Maintenance of the history of FOIs is done by storage modules in SensorHub._


### Network Output

For sensor networks, the observation output(s) should include a field indicating the ID of the entity that generated the data record. For instance, the record description for weather measurements coming from a network of weather station could be such as:

```xml
<swe:DataRecord definition="http://sensorml.com/ont/swe/property/WeatherData">
    <swe:field name="time">
        <swe:Time
            definition="http://www.opengis.net/def/property/OGC/0/SamplingTime" referenceFrame="http://www.opengis.net/def/trs/BIPM/0/UTC">
            <swe:label>Sampling Time</swe:label>
            <swe:uom xlink:href="http://www.opengis.net/def/uom/ISO-8601/0/Gregorian"/>
        </swe:Time>
    </swe:field>
    <swe:field name="stationID">
        <swe:Text definition="http://sensorml.com/ont/swe/property/StationID">
            <swe:label>Station ID</swe:label>
        </swe:Text>
    </swe:field>
    <swe:field name="temp">
        <swe:Quantity definition="http://sensorml.com/ont/swe/property/Temperature">
            <swe:label>Air Temperature</swe:label>
            <swe:uom code="degF"/>
        </swe:Quantity>
    </swe:field>
    <swe:field name="humidity">
        <swe:Quantity definition="http://sensorml.com/ont/swe/property/HumidityValue">
            <swe:label>Relative Humidity</swe:label>
            <swe:uom code="%"/>
        </swe:Quantity>
    </swe:field>
    <swe:field name="press">
        <swe:Quantity definition="http://sensorml.com/ont/swe/property/AirPressureValue">
            <swe:label>Atmospheric Pressure</swe:label>
            <swe:uom code="[in_i]Hg"/>
        </swe:Quantity>
    </swe:field>
</swe:DataRecord>
```

Notice the field `stationID' whose value would be the ID of the entity/station that generated the record.

