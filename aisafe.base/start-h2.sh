#!/bin/bash
cd "$(dirname "$0")"
echo "Starting H2 TCP server..."
echo "Database stored in: $(pwd)/db/"
echo "Connection URL: jdbc:h2:tcp://localhost:9093/./db/aisafe"
echo "Press Ctrl+C to stop."
mvn exec:java -Ph2-server
