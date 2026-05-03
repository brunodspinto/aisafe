#!/bin/bash
cd "$(dirname "$0")"
echo "persistence.repositoryFactory=aisafe.infrastructure.persistence.inmemory.InMemoryRepositoryFactory" > src/main/resources/application.properties
mvn clean compile exec:java
