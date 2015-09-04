#!/bin/bash
java -Xmx128m -cp "lib/*" org.sensorhub.impl.SensorHub config.json db
