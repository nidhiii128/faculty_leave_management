# Task 10 — Continuous Testing in Jenkins

## 1. Selenium suite in the pipeline

`Jenkinsfile` stage order:

```
Checkout → Build → Unit tests → Selenium quality gate → Package → (Docker) → Deploy → Smoke test
```

```groovy
stage('Selenium quality gate') {
    steps {
        sh "${MVN} verify -DskipUnitTests -Dbrowser=${params.BROWSER} -Dapp.env=${params.APP_ENV}"
    }
    post {
        always {
            junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: false
            archiveArtifacts artifacts: 'target/selenium-screenshots/**', allowEmptyArchive: true
        }
    }
}
```

- `-DskipUnitTests` (a pom property bound to Surefire's `skipTests`) avoids running unit tests twice.
- Failsafe's `verify` goal fails the build when any `*UiIT` fails → later stages (**Package, Deploy**)
  are skipped automatically. That is the quality gate.
- `junit` publishes results to *Test Result* and the trend chart; failure evidence is archived as build artefacts.
- `BROWSER` parameter: `htmlunit` (no browser needed) or `chrome`/`firefox` if installed on the agent.

## 2. Deliberately introduced defect and correction

The defect was chosen so that **unit tests pass but the UI is wrong**, which shows why UI tests are
needed as a gate.

| Step | Commit | Result |
|---|---|---|
| 1 | `ci: Selenium quality gate stage ...` | Pipeline green |
| 2 | `chore: [Task 10 defect demo] deliberately introduce dashboard counter defect` — `dashboard.html` shows `summary.approved` in the **Pending** tile | Unit tests **24/24 pass**; Selenium **1 failure** → `BUILD FAILURE`, deploy skipped |
| 3 | `fix: dashboard Pending tile shows the pending count` | Unit 24/24, Selenium 14/14 → green, deployed |

Failing test (`docs/evidence/task10-defect-failed-run.txt`):

```
[selenium] failure evidence saved: .../target/selenium-screenshots/ApprovalWorkflowUiIT_hodApprovesLeave.*
[ERROR] ApprovalWorkflowUiIT.hodApprovesLeave:34
expected: -1
 but was: 1
[ERROR] Tests run: 14, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

After approving, the test expects *pending* to drop by 1, but the tile showed the approved count (which rose by 1).
The saved page snapshot is `docs/evidence/task10-failure-snapshot.html`.
Successful rerun: `docs/evidence/task10-fixed-successful-run.txt`.

### Reproduce it in Jenkins

```bash
git checkout develop
git revert --no-edit <fix-commit>      # re-introduces the defect
git push                               # Jenkins build N: RED at "Selenium quality gate", Deploy skipped
git revert --no-edit HEAD              # corrects it again
git push                               # Jenkins build N+1: GREEN, deployed
```

## 3. Evidence to capture

- Stage View: build N red at *Selenium quality gate*, *Package/Deploy* grey (skipped).
- *Test Result* page of build N showing `ApprovalWorkflowUiIT.hodApprovesLeave` failed; archived snapshot.
- Defect correction commit on GitHub; build N+1 all green.
