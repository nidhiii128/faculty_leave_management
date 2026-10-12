# Task 1 — Problem Definition and Scope

**Project:** Automated Testing Pipeline for a Faculty Leave Management System
**Student:** Nidhi Bhawari (Roll No. 23102A0006), BE Computer Engineering, Div A, Batch 1
**Academic Year:** 2026-27 (ODD Semester)

## 1. Real-time need

In many departments, faculty leave is still applied on paper forms or by e-mail. The form goes to the
Head of Department (HOD), who signs it, and the office keeps a register. This causes:

| Pain point | Who is affected | Effect |
|---|---|---|
| Paper/e-mail applications get lost or delayed | Faculty, HOD | Leave approved late or not at all |
| No single view of who is on leave on a given day | HOD, timetable coordinator | Lectures are left uncovered |
| Faculty cannot see the status of their request | Faculty | Repeated follow-ups with office |
| Manual register is error-prone | Office staff | Wrong leave balance, audit issues |
| Software changes are deployed by hand with no tests | Developers, users | Bugs reach production, downtime |

The last row is the **DevOps focus** of this project: the application is small, but every change to it
must be built, tested by Selenium, packaged in Docker and deployed automatically, so that a broken
change can never reach users.

## 2. Target users and stakeholders

| Stakeholder | Role in the system | Interest |
|---|---|---|
| Faculty member | Primary user | Apply, edit, cancel, track own leave |
| Head of Department (HOD) | Approver | Approve / reject, see department summary |
| Office / Admin staff | Indirect user | Reliable record of approved leave |
| Timetable coordinator | Indirect user | Know who is absent to arrange substitutes |
| Developer (student) | Builds and maintains | Fast, safe, repeatable releases |
| Operations / Lab admin | Runs the server | Easy, repeatable provisioning and rollback |
| Project guide / examiner | Evaluator | Evidence of a complete DevOps lifecycle |

## 3. Objectives

1. Build a web-based leave application with role-based approval (Faculty → HOD).
2. Keep all source in Git with a branching and pull-request workflow.
3. Automate build and packaging with Maven and Jenkins (CI).
4. Automate browser testing with Selenium and use it as a **quality gate** — failed tests stop deployment.
5. Package the app as a Docker image and deploy it automatically after tests pass (CD).
6. Provision the server with Ansible in an idempotent way, with health check and rollback.

## 4. Constraints

- Academic timeline: 15 lab tasks (one semester).
- Single developer; free/open-source tools only (Git, GitHub, Maven, Jenkins, Selenium, Docker, Ansible).
- Runs on a laptop / lab machine (Windows + WSL2 Ubuntu or Linux VM), modest RAM (≈8 GB).
- No real HR/LDAP integration — demo users are configured in the application.
- Embedded H2 database (no separate DB server needed).

## 5. Measurable success criteria

| # | Criterion | Target |
|---|---|---|
| S1 | MVP features working (create, view, update, search, approve/reject, dashboard) | 100 % of MVP stories accepted |
| S2 | Automated unit + Selenium test pass rate on `main` | 100 % |
| S3 | Selenium critical journeys covered | ≥ 5 journeys |
| S4 | Time from `git push` to running container | < 10 minutes, zero manual steps |
| S5 | A failing test blocks deployment | Demonstrated in Jenkins (Task 10) |
| S6 | Re-running Ansible on a configured host | `changed=0` (idempotent) |
| S7 | Rollback to previous release | < 2 minutes, health check green |

## 6. Approved MVP scope (frozen)

**In scope**

- Login with roles FACULTY and HOD.
- Faculty: apply for leave (type, dates, reason), view own records, edit while *Pending*, cancel while *Pending*.
- HOD: view all records, approve, reject with a comment.
- Search/filter records by keyword, status and leave type.
- Summary dashboard: counts by status, approved leave days, upcoming approved leave.
- Read-only JSON API for records and summary; health endpoint for monitoring.

**Out of scope (future work)**

- Leave balance accounting per policy, holiday calendar, half-day leave.
- E-mail/SMS notifications, multi-level approval (Principal), LDAP/SSO login.
- Separate production database server (MySQL/PostgreSQL), Kubernetes.

**Status:** Scope frozen for the 15-task plan (see `02-agile-plan.md`).
