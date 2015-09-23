Download Open Sensor Hub
---


### Releases

Binary and Source distributions archives can be downloaded directly from the [Releases Section](https://github.com/sensiasoft/sensorhub/releases) of our GitHub account.

You'll soon find there pre-configured distributions for the most common devices such as:

- Android
- Raspberry Pi
- Desktop Linux
- Windows

See the [Install Section](install.html) for instructions on how to set it up on your device.


### Maven

You can also use Maven to include OSH in your own project. 
For instance, if you want to develop a new sensor driver, you can simply add a dependency to the SensorHub Core module in your POM:

```xml
<dependency>
   <groupId>org.sensorhub</groupId>
   <artifactId>sensorhub-core</artifactId>
   <version>1.0</version>
   <type>bundle</type>
</dependency> 
```

However, SensorHub is not available from Maven Central yet, so you'll also have to include the following repository in your POM:

```xml
<repositories>
   <repository>
      <id>sensiasoft</id>
      <url>http://sensiasoft.net/maven-repo</url>
   </repository>
</repositories>   
```

Our Maven repository is also an OSGI Bundle Repository, so you can also use any OSGI implementation to download Bundles dynamically.