SWE Common / SensorML API
---

In SensorHub, sensor descriptions (or sensor metadata) are in the [SensorML 2.0](http://www.opengeospatial.org/standards/sensorml) format, an international open standard from the [Open Geospatial Consortium (OGC)](http://www.opengeospatial.org). They are often generated (at least partly) from code using the java SensorML bindings included in [lib-sensorml](https://github.com/sensiasoft/lib-sensorml).

These bindings are automatically generated from the 2.0 XML schemas and thus are a direct reflection of the types and properties that are defined by it. The general rule is that each [XML Schema Complex Type](http://www.w3.org/TR/xmlschema-1/#Complex_Type_Definitions) (except OGC Property Types) becomes a Java interface with appropriate methods to handle each property (get/set/isSet/unSet, getNum/add for multiplicity > 1, etc.).

There is one subtle difference compared to other bindings that could be generated with commonly used tools such as JAXB or XML Beans: OGC Property Types are not generated as separate objects thus removing many unnecessary layers in the generated object tree. Instead, properties are handled as a generic [_OgcProperty_](https://github.com/sensiasoft/lib-swe-common/blob/master/swe-common-core/src/main/java/net/opengis/OgcProperty.java) object, containing all info carried by the property such as name, xlink attributes, etc., and accessible via _'getProperty'_ methods. This means that calls to regular get methods would return the property value directly which makes constructing the object much more straight forward. This design allows for handling the entire content model from many OGC schemas without making the resulting object tree too complex.

Let's look at examples of how to set different parts of a SensorML document using this API (All code in the following section assumes you have an instance of _PhysicalComponent_ or _PhysicalSystem_ called _'system'_ on hand).

_Note: Most of the following examples are actually extracted from the 'testGenerateInstance()' method of this [JUnit Test Class](https://github.com/sensiasoft/lib-sensorml/blob/master/sensorml-core/src/test/java/org/vast/sensorML/test/TestSMLBindingsV20.java) so you can look at the code directly_


### High-Level Descriptive Info

The first thing you need to do to create or add to a SensorML description is to instantiate SML and SWE helperfactories:

```java
SMLHelper smlFac = new SMLHelper();
SWEHelper sweFac = new SWEHelper();
```

Then, if you don't already have one, create the top level process or system instance. For instance a `PhysicalSystem` entity is created like so:

```java
PhysicalSystem system = smlFac.newPhysicalSystem();
```

You can then set name and description of the system:

```java
system.setName("Garage Thermometer");
system.setDescription("Thermometer located next to the door inside my garage");
```

You can also set the parent type of this sensor (this is typically used to reference a SensorML description providing more details about the sensor such as the SensorML document/datasheet provided by the manufacturer):

```java
system.setTypeOf(new ReferenceImpl("http://manufacturer.org/datasheets/sensor1234.xml"));
```


### Advanced Metadata 

Add contact information:

```java
ContactList contacts = smlFac.newContactList();
CIResponsibleParty contact = smlFac.newResponsibleParty();
contact.setIndividualName("Gérard Blanquet");
contact.setOrganisationName("Time Soft S.A.");
contact.getContactInfo().getAddress().addDeliveryPoint("10 rue du Nord");
contact.getContactInfo().getAddress().setPostalCode("75896");
contact.getContactInfo().getAddress().setCity("Paris");
contact.getContactInfo().getAddress().setCountry("FRANCE");
contact.setRole(new CodeListValueImpl("operator"));
contacts.addContact(contact);
system.addContacts(contacts);
```

Add characteristics:

```java
CharacteristicList mechSpecs = smlFac.newCharacteristicList();
Quantity weightSpec = sweFac.newQuantity("http://sweet.jpl.nasa.gov/2.3/propMass.owl#Mass", "Weight", null, "kg");
weightSpec.setValue(12.3);
mechSpecs.addCharacteristic("weight", weightSpec);
system.addCharacteristics("mechanical", mechSpecs);
```


### Location

Add location as GML point:

```java
GMLFactory gmlFac = new GMLFactory();
Point pos = gmlFac.newPoint();
pos.setId("P01");
pos.setSrsName("http://www.opengis.net/def/crs/EPSG/0/4979");
pos.setPos(new double[] {45.6, 2.3, 193.2});
system.addPositionAsPoint(pos);
```


### Inputs/Outputs/Parameters

All inputs, outputs and parameters in SensorML are described using the SWE Common Language so you can use the `SWEHelper` class to create these structures. 

Add observable property as input:

```java
ObservableProperty obs = new ObservablePropertyImpl();
obs.setDefinition("http://mmisw.org/ont/cf/parameter/weather");
system.addInput("weather_phenomena", obs);
```

You can also add an input as xlink reference:

```java
system.getInputList().add("rain", "http://remotedef.xml", null);
```

Add output record (in this case we first create the record object and then add sub-components to it, before we add it as output):

```java
// create output record and set description
DataRecord rec = sweFac.newDataRecord();
rec.setLabel("Weather Data Record");
rec.setDescription("Record of synchronous weather measurements");

// sampling time
rec.addField("time", sweFac.newTimeStampIsoUTC());

// temperature measurement
rec.addField("temp", sweFac.newQuantity(
                            "http://mmisw.org/ont/cf/parameter/air_temperature", 
                            "Air Temperature", null, "Cel"));

// pressure
rec.addField("press", sweFac.newQuantity(
                            "http://mmisw.org/ont/cf/parameter/air_pressure_at_sea_level",
                            "Air Pressure", null, "mbar"));

// wind speed
rec.addField("wind_speed", sweFac.newQuantity(
                            "http://mmisw.org/ont/cf/parameter/wind_speed",
                            "Wind Speed", null, "km/h"));
        
// wind direction
rec.addField("wind_dir", sweFac.newQuantity(
                            "http://mmisw.org/ont/cf/parameter/wind_to_direction",
                            "Wind Direction", null, "deg"));

// add as output
system.addOutput("weather_data", rec);
```

You can also add accuracy info to some of the measured outputs:

```java
// add accuracy info to temp output
Quantity acc = sweFac.newQuantity(
                      "http://mmisw.org/ont/cf/parameter/accuracy",
                      "Accuracy", null, "%");
(Quantity)rec.getField("temp")).addQuality(acc);
```

Parameters can be added in a similar fashion:

```java
system.addParameter("samplingPeriod", sweFac.newQuantity(
                       "http://sensorml.com/ont/swe/property/SamplingPeriod",
                       "Sampling Period", null, "s"));
```


### Reference Frames

One important information that can be added to a sensor or system description is documentation about the reference frame that is attached to it. This is useful for properly processing positioning information in advanced geolocation workflows. Below is an example spatial reference frame definition:

```java
SpatialFrame systemFrame = smlFac.newSpatialFrame();
systemFrame.setId("SYSTEM_FRAME");
systemFrame.setLabel("System Reference Frame");
systemFrame.setDescription("Cartesian reference frame attached to system assembly");
systemFrame.setOrigin("Origin is located on the red marking at the bottom of the aluminum chassis");
systemFrame.addAxis("x", "X axis is aligned with the horizontal edge of the chassis (see marking)");
systemFrame.addAxis("y", "Y axis is orthogonal to both X and Y in order to form a direct orthogonal frame");
systemFrame.addAxis("z", "Z axis is pointing toward the top of the assembly, aligned with the vertical edge of the aluminum frame");
system.addLocalReferenceFrame(systemFrame);
```


### Write-out as XML

Once you have the java object tree created, it is trivial to serialize it as XML that is compliant to the SensorML standard:

```java
new SMLUtils().writeProcess(System.out, system, true);
```


