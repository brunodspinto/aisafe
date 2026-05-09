#!/bin/bash
cd "$(dirname "$0")"
echo "persistence.repositoryFactory=aisafe.infrastructure.persistence.inmemory.InMemoryRepositoryFactory" > src/main/resources/application.properties
mvn clean compile -q
java -cp target/classes:$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout 2>/dev/null) aisafe.app.console.AiSafeBootstrap
