# US005 - Automated Deployment Scripts

## 1. Requirements Engineering

### 1.1 User Story Description

> As Project Manager, I want the team to add to the project the necessary scripts, so that build/executions/deployments can be executed effortlessly in a Unix compatible machine. Include scripts for all the major tasks and execution of applications.

### 1.2 Customer Specifications and Clarifications

**From the project specifications:**

>

### 1.3 Acceptance Criteria

> No acceptance criteria specified.

### 1.4 Found out Dependencies

* There are no dependencies for this user story as it is a project infrastructure task.

### 1.5 Input and Output Data

**Input Data:**
* Optional parameters per script (e.g. host, port, profile)

**Output Data:**
* Build/execution success or failure messages
* (In)success of the operation

### 1.6 System Sequence Diagram (SSD)

![System Sequence Diagram](svg/US005-SSD.svg)

### 1.7 Other Relevant Remarks

* For Sprint A (US005 scope), scripts are in `libs/scripts`
* All scripts must have execute permissions (`chmod +x`)
* Scripts must use relative paths based on their location so they work from any directory
* The `build.sh` script must trigger both Java (Maven) and C builds
* The necessary Sprint A scripts are `build.sh`, `build_c.sh`, `clean.sh`, `generate-plantuml-diagrams.sh`, and `run_placeholder.sh`
* Scripts must be compatible with `bash`
* `generate-plantuml-diagrams.sh` requires Java 11+ to process PlantUML files (Java 8 is insufficient for the PlantUML jar version 1.2026.2)
