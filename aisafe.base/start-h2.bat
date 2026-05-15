@echo off
cd /d %~dp0
echo Starting H2 TCP server...
echo Database stored in: %CD%\db\
echo Connection URL: jdbc:h2:tcp://localhost/./db/aisafe
echo Press Ctrl+C to stop.
mvn exec:java -Ph2-server
