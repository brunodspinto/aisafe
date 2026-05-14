#!/bin/bash
cd "$(dirname "$0")"
echo "Starting H2 TCP server..."
echo "Database stored in: $(pwd)/db/"
echo "Connection URL: jdbc:h2:tcp://localhost/./db/aisafe"
echo "Press Ctrl+C to stop."
mvn exec:java \
    -Dexec.mainClass="org.h2.tools.Server" \
    -Dexec.classpathScope="runtime" \
    -Dexec.args="-tcp -tcpAllowOthers -ifNotExists -baseDir ./db"
