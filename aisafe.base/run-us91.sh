#!/bin/bash
mvn compile -q
CP=$(mvn dependency:build-classpath -DforceStdout -q 2>/dev/null)
java -cp "target/classes:$CP" aisafe.app.loggingserver.RemoteAccessLoggingServerApp
