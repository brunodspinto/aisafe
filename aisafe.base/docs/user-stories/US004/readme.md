# US004 - Continuous Integration Server

## 1. Context
This task was assigned in Sprint 1 and establishes the foundations for the quality control of the AISafe project's code. It is being developed for the first time, and its goal is to ensure that all changes submitted by the team keep the system in a compilable and functional state from day one.

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
4. Invoke the Maven engine to clean previous builds, recompile all the code, and execute the entire test suite (`mvn -B clean verify`).
5. Publish test results and JaCoCo coverage reports as GitHub Actions artifacts (NFR05: "publishing of results and metrics").

All the team's domain, application, and infrastructure code lives in the `aisafe.base/` Maven project, so the CI builds `aisafe.base/pom.xml`. The EAPLI framework is resolved from remote repositories, so no other local project is needed. The CI also builds the C simulation and runs its unit tests (`make test`).

## 4. Design
The solution's architecture is based on declaring infrastructure as code (IaC) through a YAML configuration file. GitHub requires this file to be placed in a specific, hidden directory: `.github/workflows/`, which must be located at the root of the repository.

The schedule for the *night builds* will use the cron expression `0 2 * * *`, instructing GitHub servers to run the script every day at 02:00 AM.

To satisfy NFR05's requirement to publish results and metrics:
- **Test results** (Surefire XML reports) are uploaded as a GitHub Actions artifact named `test-results`.
- **Coverage report** (JaCoCo HTML/XML) is uploaded as a GitHub Actions artifact named `coverage-report`. JaCoCo is configured in `aisafe.base/pom.xml` with both `prepare-agent` (instruments bytecode before tests) and `report` (generates the HTML/XML report in the `verify` phase).

## 5. Implementation
The `.github/workflows/maven-ci.yml` file at the root of the project:

```yaml
name: AISafe CI Build

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
      run: mvn -B clean verify --file aisafe.base/pom.xml

    - name: Build C Simulation and Run C Tests
      working-directory: aisafe.base/simulation
      run: |
        make all
        make test

    - name: Upload Test Results
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: test-results
        path: aisafe.base/target/surefire-reports/

    - name: Upload Coverage Report
      if: always()
      uses: actions/upload-artifact@v4
      with:
        name: coverage-report
        path: aisafe.base/target/site/jacoco/
```

The `if: always()` condition on the upload steps ensures that test results and coverage data are published even when tests fail, allowing the team to inspect failures without re-running the build.