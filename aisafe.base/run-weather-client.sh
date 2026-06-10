#!/bin/bash
cd "$(dirname "$0")"
mvn compile -q
mvn dependency:build-classpath -Dmdep.outputFile=.weather-cp -q
java -cp "target/classes:$(cat .weather-cp)" aisafe.app.weatherperson.WeatherPersonTcpClientApp
rm -f .weather-cp
