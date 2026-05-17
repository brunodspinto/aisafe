@echo off
cd /d %~dp0
set PROP=persistence.repositoryFactory=aisafe.infrastructure.persistence.jpa.JpaRepositoryFactory
> src\main\resources\application.properties.tmp echo %PROP%
if exist src\main\resources\application.properties (
	for /f "delims=" %%L in ('findstr /v /b /c:"persistence.repositoryFactory=" src\main\resources\application.properties') do echo %%L>> src\main\resources\application.properties.tmp
)
move /y src\main\resources\application.properties.tmp src\main\resources\application.properties >nul
mvn clean compile exec:java
