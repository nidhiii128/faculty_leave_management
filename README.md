# Faculty Leave Management System — Automated Testing Pipeline

DevOps lab project (BE Computer Engineering, Sem VII, 2026-27) — **Nidhi Bhawari, Roll No. 23102A0006**.

A small web application where faculty apply for leave and the HOD approves or rejects it, delivered
through a complete DevOps pipeline: Git → Jenkins → Maven → Selenium quality gate → Docker → Ansible.

```
git push → Jenkins → Build → Unit tests → Selenium gate → Package WAR → Docker image → Registry → Deploy → Smoke test
                                               │ fail
                                               └──→ stop: nothing is deployed
```

## Features

- Login with Faculty and HOD roles
- Faculty can apply for leave and view their own records
- Faculty can edit or cancel pending requests; HOD approves or rejects with a comment
- Search by keyword, status and type; summary dashboard; JSON API
- **Leave calendar**: month view of who is on leave each day (`/calendar`)
- **Supporting documents**: attach a medical certificate (PDF/JPG/PNG, up to 5 MB); only the owner and HOD can open it
- **Substitute arrangement**: name a colleague who takes your lectures; they see it on their dashboard
- VIT-themed responsive design: landing page, split login, dashboard with charts
- Health and version endpoints (`/actuator/health`, `/actuator/info`)

## Quick start

Requires JDK 17+ and Maven 3.8+.

```bash
mvn spring-boot:run
```

Open http://localhost:8080 and sign in as `faculty1`, `faculty2` or `faculty3` / `password` (Faculty) or `hod` / `password` (HOD).

| Command | What it does |
|---|---|
| `mvn test` | 33 unit and MVC tests |
| `mvn verify` | unit tests + 17 Selenium tests (headless HtmlUnit) |
| `mvn verify -Dbrowser=chrome` | Selenium tests in headless Chrome |
| `mvn package && java -jar target/flms.war` | Build and run the WAR |
| `docker compose up -d --build` | Run in Docker on http://localhost:8090 (after `mvn package`) |
| `cd jenkins && docker compose up -d --build` | Jenkins (:8081) + Tomcat (:8888) |
| `cd ansible && ansible-playbook site.yml -K` | Provision a host and deploy |

## Repository layout

| Path | Contents |
|---|---|
| `src/main/java/com/flms` | `config`, `model`, `repository`, `service`, `web` |
| `src/test/java/com/flms` | `service` + `web` unit tests, `ui` Selenium journeys |
| `Jenkinsfile` | CI/CD pipeline |
| `Dockerfile`, `docker-compose.yml` | Container image and local run |
| `jenkins/` | Jenkins + Tomcat lab environment |
| `ansible/` | Provisioning, deployment, rollback |
| `docs/` | One document per lab task, evidence |

## Documentation

| Task | Document |
|---|---|
| 1 | [Problem definition and scope](docs/01-problem-definition.md) |
| 2 | [Agile plan and DevOps workflow](docs/02-agile-plan.md) |
| 3 | [SRS, architecture and setup](docs/03-srs-architecture.md) |
| 4 | [Git and GitHub workflow](docs/04-git-workflow.md) |
| 5–6 | [Feature branches, conflict resolution, release tag](docs/05-06-feature-development.md) |
| 7 | [Jenkins installation and CI job](docs/07-jenkins-ci.md) |
| 8 | [Jenkinsfile and Tomcat deployment](docs/08-pipeline-deployment.md) |
| 9 | [Selenium test plan and local execution](docs/09-selenium-tests.md) |
| 10 | [Continuous testing — quality gate and defect correction](docs/10-continuous-testing.md) |
| 11 | [Docker image and container lifecycle](docs/11-docker.md) |
| 12 | [Jenkins → Docker continuous deployment](docs/12-jenkins-docker-cd.md) |
| 13 | [Configuration management (Ansible)](docs/13-configuration-management.md) |
| 14 | [Provisioning, idempotency, health check, rollback](docs/14-provisioning-reliability.md) |
| 15 | [Final report, troubleshooting, viva](docs/15-final-report.md) |
| — | [Evidence checklist](docs/EVIDENCE-CHECKLIST.md) |

Built and tested automatically by Jenkins on every push to `develop`.
Polling schedule: every 5 minutes (H/5 * * * *).
