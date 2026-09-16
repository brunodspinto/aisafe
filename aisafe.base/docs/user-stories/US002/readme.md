# US002 - Project Repository

## 1. Context
This task was assigned during Sprint 1 and represents the foundational step for the team's collaboration, version control, and agile management. Since the project adopts a Project-Based Learning (PBL) methodology with Scrum, establishing a robust environment for code sharing and task tracking is mandatory before any development begins.

## 2. Requirements
**US002:** As Project Manager, I want the team to use the defined project repository in GitHub and setup a GitHub tool for project management.

**Acceptance Criteria:**
- US002.1. The team must utilize the official GitHub repository provided by the course coordinators.
- US002.2. A native GitHub project management tool (GitHub Projects) must be configured to support the Scrum framework.
- US002.3. All User Stories (US) and technical tasks must be mapped as Issues and tracked through the project board.

**Dependencies/References:**
This is a baseline requirement. All subsequent User Stories depend on the correct configuration of this infrastructure so their progress can be properly tracked.

## 3. Analysis
To address this need, the team analyzed the tools available within the GitHub ecosystem. To maintain high cohesion between the source code and task management, the team opted to use **GitHub Projects**.

Using a native Kanban board on GitHub provides centralized access for both the development team and the Product Owner/Project Manager (teachers) to audit the sprint's progress, allowing commits to be directly linked to the business requirements.

## 4. Design
The solution consists of configuring a Kanban-style board in GitHub Projects. The board was designed to reflect the direct workflow adopted by the team.

The defined column (status) structure is as follows:
- **Product Backlog:** Contains all User Stories mapped for the entire project.
- **Sprint Backlog (To Do):** Tasks selected for the current active sprint.
- **In Progress:** Tasks currently being developed by a team member.
- **Testing:** Tasks whose base implementation is finished, currently in the local testing phase (unit, integration, and acceptance criteria validation) before code submission.
- **Done:** Completed tasks, validated in the testing phase, whose code has been successfully pushed directly to the `main` branch.

## 5. Implementation
The following actions were performed to implement the design:
1. The repository was successfully cloned and linked to all team members' local environments.
2. A new GitHub Projects board was created and linked directly to the `AISafe` repository.
3. Custom columns were configured according to the single-branch development process, with a strong emphasis on the *Testing* phase.
4. GitHub Issues were created for all User Stories assigned to Sprint 1, assigned to their respective team members, and tagged with appropriate labels (e.g., `documentation`, `enhancement`, `US`).

## 6. Integration/Demonstration
The project management tool is fully integrated and active. The Product Owner and the team can view the current state of the sprint by navigating to the "Projects" tab in the GitHub repository. The final transition of cards from "Testing" to "Done" can be done manually or automated using keywords in the commit messages pushed to `main` (e.g., `git commit -m "Implement feature X, closes #IssueNumber"`).

## 7. Observations
The team opted for a continuous integration strategy based on a single branch (`main`), dispensing with the use of Pull Requests. Given this direct approach, the inclusion of the **Testing** phase on the board is a crucial visual mechanism. The team acknowledges that the individual responsibility to ensure the code compiles locally and passes all tests before each push is critical to maintaining the main development line's integrity and not compromising the automatic execution of builds on the server (US004).