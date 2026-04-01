# US005 - Automated Deployment Scripts

## Overview

As Project Manager, I want the team to add to the project the necessary scripts, so that build/executions/deployments can be executed effortlessly in a Unix compatible machine. Include scripts for all the major tasks and execution of applications.

## Scripts

| Script                          | Description                                 |
|---------------------------------|---------------------------------------------|
| `build.sh`                      | Builds the entire project (Java + C)        |
| `build_c.sh`                    | Builds only the C components                |
| `clean.sh`                      | Removes all build artifacts                 |
| `generate-plantuml-diagrams.sh` | Generates SVG diagrams from all .puml files |

For Sprint A (US005), these are the only necessary scripts currently in scope in `libs/scripts`.
Real application run/database scripts are out of scope for now.

## How to Use

### Make scripts executable
```bash
chmod +x libs/scripts/*.sh
```

### Build everything
```bash
./libs/scripts/build.sh
```

### Build C components only
```bash
./libs/scripts/build_c.sh
```

### Clean build artifacts
```bash
./libs/scripts/clean.sh
```

### Generate diagrams
```bash
./libs/scripts/generate-plantuml-diagrams.sh
```


## Documentation

- [Requirements](01.requirements-engineering/US005-requirements.md)
- [Analysis](02.analysis/US005-analysis.md)
- [Design](03.design/US005-design.md)
- [Tests and Implementation](04.tests-and-implementation/US005-tests-an-implementation.md)
