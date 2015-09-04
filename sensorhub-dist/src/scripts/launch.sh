#!/bin/bash
java -Xmx128m -cp "lib/*" -Djava.system.class.loader="org.sensorhub.utils.NativeClassLoader" org.sensorhub.impl.SensorHub config.json db
