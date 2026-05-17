@echo off
cd /d %~dp0
echo persistence.repositoryFactory=aisafe.infrastructure.persistence.jpa.JpaRepositoryFactory> src\main\resources\application.properties
mvn clean compile exec:java
