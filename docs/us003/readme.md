# US003 - Project Structure

## 1. Context
This task was assigned during Sprint 1 and constitutes the initial and fundamental step of preparing the team's work environment for the AlSafe project. The objective is to adopt the provided base project (EAPLI Base Project), perform the initial configurations of the dependency manager (Maven), and organize the directory tree to independently accommodate the technologies of the different course units (Java, C, and ANTLR).

## 2. Requirements
**US003:** As Project Manager, I want the team to setup the repository structure, adopting the provided EAPLI base project, and configure the project to support Java, C, and ANTLR (LPROG).

**Acceptance Criteria:**
- US003.1. The repository must contain the base project's code structured in modules.
- US003.2. The compilation of the Java project must be guaranteed using version 21 of the language (NFR04).
- US003.3. There must be isolated and prepared spaces in the repository for the future development of the C simulator and the lexical/syntactical analyzer.

**Dependencies/References:**
This task serves as the foundation for US004 (Continuous Integration), as the CI server requires a properly configured `pom.xml` file to be able to validate the code compilation.

## 3. Analysis
The team analyzed the base project (`eapli.base`) and the required technical specifications. Since the AlSafe system will not be limited to the Java language, it was decided to adopt a technological isolation approach by directories. The source code written in C should not coexist in the same folders as the Java domain, requiring its own independent build process (`Make`).

Regarding Java, the team analyzed the Maven configuration file (`pom.xml`) to ensure that all vital dependencies and tools were present, updated, and targeted to the versions required by the business rules.

## 4. Design
To fulfill the outlined architecture, the design focused on two main fronts:
1. **Directory Tree:**
    * Creation of an independent directory named `simulation/` at the root of the repository to host the future `.c` files and their respective `Makefile`.
    * Creation of a specific directory (`src/main/antlr4`) within the main module to centralize the `.g4` grammars of the rules engine.
2. **Maven Configuration (`pom.xml`):**
    * Explicit definition of the property `<maven.compiler.release>21</maven.compiler.release>`.
    * Integration of Lombok to minimize boilerplate code (getters, setters, builders).
    * Configuration of the unit testing framework for JUnit 5 (Jupiter/Vintage).
    * Addition of audit and metrics plugins (JaCoCo) to support future code coverage tests.

## 5. Implementation
The folder structure was generated and accompanied by empty `.gitkeep`.

The main `pom.xml` file was updated and consolidated. Below are the crucial build environment definitions and some essential dependencies injected into the configuration:

```xml
    <properties>
        <m2e.apt.activation>jdt_apt</m2e.apt.activation>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

        <maven.compiler.release>21</maven.compiler.release>

        <sonar.jacoco.reportPaths>target/jacoco.exec</sonar.jacoco.reportPaths>
        <sonar.exclusions>src/main/java/**/*/package-info.java</sonar.exclusions>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>5.10.1</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>2.2.224</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.30</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>