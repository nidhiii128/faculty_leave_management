# Task 4 — Git and GitHub Repository Initialization

## 1. Create and push the repository

```bash
cd faculty-leave-devops
git init -b main
git add .
git commit -m "chore: initial project skeleton"
gh repo create faculty-leave-devops --public --source . --remote origin --push
# (or create it on github.com, then:)
# git remote add origin https://github.com/<user>/faculty-leave-devops.git
# git push -u origin main
git checkout -b develop && git push -u origin develop
```

## 2. Folder structure

```
faculty-leave-devops/
├── .github/               issue + pull-request templates
├── ansible/               configuration management (Tasks 13–14)
├── docs/                  one document per lab task
├── jenkins/               Jenkins server setup (Task 7)
├── src/main/java/com/flms config, model, repository, service, web
├── src/main/resources     application.properties, templates, css
├── src/test/java/com/flms service + MVC tests, ui/ Selenium tests
├── Dockerfile             container image (Task 11)
├── Jenkinsfile            pipeline as code (Tasks 8, 10, 12)
└── pom.xml                Maven build
```

## 3. Branch policy

| Branch | Purpose | Rules |
|---|---|---|
| `main` | Released, deployable code | Only merges from `develop` or `hotfix/*`; every merge is tagged `vX.Y.Z` |
| `develop` | Integration branch | Only merges from PRs; Jenkins must be green |
| `feature/<issue>-<short-name>` | New features, e.g. `feature/5-apply-leave` | Branch from `develop`, PR back to `develop` |
| `bugfix/<issue>-<short-name>` | Fixes found during development | Branch from `develop` |
| `hotfix/<short-name>` | Urgent production fix | Branch from `main`, merge to `main` and `develop` |
| `ci/<short-name>` | Pipeline / infrastructure changes | Same as feature |

Recommended GitHub settings (Settings → Branches → Add rule for `main` and `develop`):
require a pull request before merging, require 1 approval, require status checks (Jenkins) to pass,
and block force pushes.

## 4. Commit message convention (Conventional Commits)

```
<type>: <short summary in imperative mood>

feat: add leave application form
fix: reject leave when end date is before start date
test: add Selenium journey for HOD approval
ci: add Docker build stage to Jenkinsfile
docs: add Task 13 configuration specification
chore: initial project skeleton
```

## 5. Issues

Templates in `.github/ISSUE_TEMPLATE/` (feature request, bug report). Create one issue per backlog item
from `02-agile-plan.md`, label it (`feature`, `bug`, `ci`, `docs`), and reference it from commits
and PRs (`Closes #5`).

## 6. Evidence to capture

- Screenshot of the repository home page (README visible) and the URL.
- `git log --oneline --graph --all` output.
- Issues list and the branch protection rule page.
