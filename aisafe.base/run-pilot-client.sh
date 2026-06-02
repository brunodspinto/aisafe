#!/bin/bash
cd "$(dirname "$0")"
mvn compile -q
java -cp target/classes aisafe.app.pilot.PilotTcpClientApp
