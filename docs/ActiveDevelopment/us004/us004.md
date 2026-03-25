# US004 - Continuous Integration Server

## 1. Context
This task was assigned in Sprint 1 and establishes the foundations for the quality control of the AlSafe project's code. It is being developed for the first time, and its goal is to ensure that all changes submitted by the team keep the system in a compilable and functional state from day one.

## 2. Requirements
**US004:** As Project Manager, I want the team to setup a continuous integration server. GitHub Actions/Workflows should be used.

Additionally, this US directly addresses the following non-functional requirements (NFRs) stipulated for the project:
* **NFR05:** The Github repository will provide night builds with publishing of results and metrics. The project will use Maven as build automation tool.

## 3. Analysis
To address this need, the team chose to use **GitHub Actions**.

The configuration of this system requires the creation of an automated *workflow* that ensures the following behaviors:
1. React to `push` and `pull_request` events targeting the main branches (`main` or `master`).
2. Execute autonomously once a day (during the night) to strictly comply with the *night builds* directive.
3. Set up the execution environment on the cloud machines with the Java Development Kit (JDK) 21.
4. Invoke the Maven engine to clean previous builds, recompile all the code, and execute the entire test suite (`mvn clean verify`).

## 4. Design
The solution's architecture is based on declaring infrastructure as code (IaC) through a YAML configuration file. GitHub requires this file to be placed in a specific, hidden directory: `.github/workflows/`, which must be located at the root of the repository.

The schedule for the *night builds* will use the cron expression `0 2 * * *`, instructing GitHub servers to run the script every day at 02:00 AM.

## 5. Implementation
To implement the integration, the `.github/workflows/maven-ci.yml` file was created at the root of the project with the following code:

```yaml
name: AlSafe CI Build

on:
  push:
    branches: [ "master", "main" ]
  pull_request:
    branches: [ "master", "main" ]
  schedule:
    - cron: '0 2 * * *'

jobs:
  build:
    name: Build and Test
    runs-on: ubuntu-latest

    steps:
    - name: Checkout Code
      uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven

    - name: Build and Run Tests with Maven
      run: mvn -B clean verify --file eapli.base/pom.xml
```