# Tasks 5 & 6 — Feature Development with Branching, MVP Completion and Git Collaboration

## Task 5 — First core workflow on a feature branch

Feature: **faculty applies for leave and views own records** (US-01..US-04).

```bash
git checkout develop
git checkout -b feature/5-apply-leave
# ... implement entity, repository, service, controller, templates, tests ...
git add src/main/java/com/flms/model src/main/java/com/flms/repository
git commit -m "feat: add LeaveRequest entity and repository"
git add .
git commit -m "feat: faculty can apply for leave and view own records (closes #4)"
git push -u origin feature/5-apply-leave
git log --oneline -5
```

Pull request: `feature/5-apply-leave → develop` on GitHub (template in `.github/PULL_REQUEST_TEMPLATE.md`),
reviewed and merged with a merge commit (`Merge pull request #15 ...`). Then:

```bash
git checkout develop
git pull
```

## Task 6 — MVP completion, second branch, merge conflict, tag

Two branches were developed in parallel from `develop`:

| Branch | Content |
|---|---|
| `feature/6-approval-workflow` | Edit and cancel pending leave; HOD approve / reject with comment (`LeaveWorkflowService`) |
| `feature/6-search-dashboard` | Keyword / status / type search, dashboard, JSON API, seed data (`LeaveReportService`) |

Both branches changed the **same lines** of `README.md` (features list) and the
**constructor** of `LeaveController`, so merging the second one produced a real conflict:

```bash
git checkout develop
git merge --no-ff feature/6-approval-workflow     # PR #17 — clean
git merge --no-ff feature/6-search-dashboard      # PR #18 — CONFLICT
# CONFLICT (content): Merge conflict in README.md
# CONFLICT (content): Merge conflict in src/main/java/com/flms/web/LeaveController.java
git diff                                         # saved in docs/evidence/task06-merge-conflict.diff
```

**Resolution:** keep both sides — both feature lines in the README, and a constructor that injects
both `LeaveWorkflowService` and `LeaveReportService`. Verified with `mvn clean test`, then:

```bash
git add README.md src/main/java/com/flms/web/LeaveController.java
git commit            # "Merge pull request #18 ... Resolved conflict in README.md and LeaveController"
```

On GitHub the same conflict appears on the pull request as "This branch has conflicts that must be
resolved"; resolve it with **Resolve conflicts** in the web editor or locally as above.

### Release baseline

```bash
git checkout main
git merge --no-ff develop -m "Release v1.0.0: MVP"
git tag -a v1.0.0 -m "MVP: apply, view, edit, cancel, approve/reject, search, dashboard"
git push origin main develop --tags
```

### Evidence

- `git log --oneline --graph --all` (shows the three merged PRs and the conflict merge)
- `docs/evidence/task06-merge-conflict.diff` (the conflict as Git showed it)
- `git show v1.0.0`
- Screenshots: PR pages on GitHub, the running MVP (dashboard, list with search, approve screen)

### Updated backlog

US-01 to US-10 → **Done**. Remaining: US-11 (CI/CD, Tasks 7–12) and US-12 (provisioning, Tasks 13–14).
