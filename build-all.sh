#!/bin/bash
mvn clean install
mvn clean install -P sensors
mvn clean install -P webui
