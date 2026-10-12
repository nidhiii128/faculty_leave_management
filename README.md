# Faculty Leave Management System — Automated Testing Pipeline

DevOps lab project (BE Computer Engineering, Sem VII, 2026-27) — **Nidhi Bhawari, Roll No. 23102A0006**.

A small web application where faculty apply for leave and the HOD approves or rejects it, delivered
through a complete DevOps pipeline: Git → Jenkins → Maven → Selenium quality gate → Docker → Ansible.

## Features

- Login with Faculty and HOD roles
- Faculty can apply for leave and view their own records
- Faculty can edit or cancel pending requests; HOD approves or rejects with a comment

## Quick start

Requires JDK 17+ and Maven 3.8+.

```bash
mvn spring-boot:run
```

Open http://localhost:8080 and sign in as `faculty1` / `password` (Faculty) or `hod` / `password` (HOD).

## Documentation

| Task | Document |
|---|---|
| 1 | [Problem definition and scope](docs/01-problem-definition.md) |
| 2 | [Agile plan and DevOps workflow](docs/02-agile-plan.md) |
| 3 | [SRS, architecture and setup](docs/03-srs-architecture.md) |
| 4 | [Git and GitHub workflow](docs/04-git-workflow.md) |
