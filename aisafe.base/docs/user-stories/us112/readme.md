# US112 - Monthly Report Generation

## 1. Context

US112 introduces a Java-side reporting capability for the AISafe backoffice. The goal is
to allow a Flight Control Operator to generate a monthly operational report that can be
reviewed regularly and reused as the foundation for future report types.

Unlike the Sprint 2 C simulation reports (US109), this story belongs to the Java
application layer and should remain aligned with the project's existing console-based,
repository-driven architecture. The expected outcome at this stage is not a complex BI
module, but a structured report generation flow that can evolve later into compliance,
incident, or other operational reports.

---

## 2. Requirements

**US112** As a Flight Control Operator, I want to generate a monthly statistics report so
that operational performance can be reviewed on a regular basis.

**Acceptance Criteria:**

- **AC112.1** The report contains monthly statistics relevant to flight operations.
- **AC112.2** The report follows a consistent branding and structure that serves as the
  foundation for all future report types (compliance, incident, etc.).
- **AC112.3** Each report type has a specific data collection method, sections and
  graphics.

**Dependencies/References:**

- **US030** - Authentication and Authorization: the operation should only be available to
  authenticated users with the appropriate role.
- **US010** - Domain Model: reporting should reuse existing domain concepts instead of
  introducing an isolated parallel model.
- Existing operational repositories already present in the Java application should be the
  preferred data source for the first version of the report.

---

## 3. Analysis

### 3.1 Problem Framing

The core challenge of US112 is not only to compute monthly numbers, but to establish a
simple reporting structure that can be reused later by other report types. The story asks
for two things at the same time:

1. A concrete **monthly statistics report** for operational review.
2. A reusable **report template/foundation** that future reports can follow.

That means the analysis must separate:

- the **common report skeleton** shared by all report types;
- the **monthly data collection rules** that are specific to US112.

### 3.2 Scope Decision

At the moment, the Java codebase contains several operational aggregates and repositories
such as flight plans, weather data, airports, aircraft, and companies, but it does not yet
contain a mature generic reporting module. Therefore, the recommended approach for a
second-year project is:

- keep report generation in the Java application layer;
- reuse existing repositories as data sources;
- produce a structured textual report first;
- avoid introducing a complex analytics or dashboard subsystem.

This keeps the solution aligned with the course level and with the existing console
application style.

### 3.3 Relevant Operational Data

For a monthly operational report, the most reasonable first data sources are the domain
records that already represent operational activity in the Java system.

The following data families are relevant:

| Data family | Why it is relevant |
|-------------|--------------------|
| Flight plans | They represent planned operational activity and approval workload |
| Weather data | They represent operational context and support activity |
| Simulation/report outputs when available | They can complement the report later with safety-oriented metrics |

Since the current Java application already has stronger support for flight-plan and weather
management than for advanced report aggregation, the first version of US112 should be based
primarily on those persisted records.

### 3.4 Monthly Statistics Candidate Set

To satisfy AC112.1 without inventing metrics that the system cannot yet support reliably,
the report should focus on counts and summaries that can be derived from repository data in
a straightforward way.

Reasonable monthly statistics include:

- number of flight plans created in the selected month;
- number of flight plans by status, especially approved and rejected;
- number of weather records added in the selected month;
- breakdowns by simple categories when already available in the domain, such as flight type
  or approval status.

These metrics are:

- operationally meaningful;
- easy to explain to the evaluator;
- realistic for the current system maturity.

### 3.5 Common Structure Required by Future Reports

AC112.2 implies that the monthly report must not be designed as a one-off artifact. A
shared structure is needed so that later reports (for example compliance or incident
reports) can reuse the same visual and logical layout.

The common structure should include:

- report title and type;
- branding/header area;
- reporting period;
- summary section;
- detailed sections;
- footer with generation date and user/context.

This structure is generic enough to be reused by future report types while still simple
enough for a console-driven academic project.

### 3.6 Report-Type Specialization

AC112.3 states that each report type has its own data collection method, sections and
graphics. The implication is that the system should not hardcode everything into a single
class or controller.

However, a full-blown reporting framework would be excessive here. A simpler interpretation
fits the course better:

- keep one common report model or template;
- let each report type provide its own statistics and section contents;
- keep graphics simple, for example textual charts or lightweight generated visuals in a
  later step.

For US112 specifically, this means the "monthly statistics" report defines:

- which repositories are queried;
- which monthly counters are computed;
- which sections appear in the final output.

### 3.7 Main Architectural Insight

The main design direction emerging from the analysis is:

- one **generic report structure**;
- one **report generator flow**;
- one **monthly statistics specialization** for the data collection rules.

This is enough to satisfy the user story while keeping the solution small, explainable, and
consistent with the project's existing architecture.
