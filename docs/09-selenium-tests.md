# Task 9 — Selenium Test Design and Local Execution

## 1. Test plan

| Item | Detail |
|---|---|
| Scope | Five critical user journeys through the real UI (browser → HTTP → Spring → H2) |
| Tool | Selenium WebDriver 4 + JUnit 5 + AssertJ, run by Maven Failsafe |
| Browsers | **HtmlUnit** (default, headless, no install — used in CI); **Chrome**/**Firefox** headless via `-Dbrowser=chrome` / `firefox` |
| Environment | `@SpringBootTest(RANDOM_PORT)` starts the app on a free port with an empty in-memory DB |
| Test data | Created by each test through the UI; reasons carry a random suffix (`unique(...)`) so tests never depend on each other |
| Entry criteria | `mvn test` (unit tests) green |
| Exit criteria | 100 % of UI tests pass; any failure fails the build (`failsafe:verify`) |
| Failure evidence | `target/selenium-screenshots/<Test>_<method>.html` (+ `.png` on Chrome/Firefox) |

Element locators are stable `id` attributes added to the templates (`leave-status`, `approve-leave`, ...),
so tests don't break when styling changes.

## 2. Critical user journeys and test cases

| Journey | Class | Test case | Expected result |
|---|---|---|---|
| 1. Login | `LoginUiIT` | Wrong password | Stays on `/login?error`, "Invalid username or password." |
| | | Valid login + logout | Dashboard with "Dr. Asha Patil"; logout message |
| | | Anonymous opens `/leaves` | Redirected to `/login` |
| 2. Apply | `ApplyLeaveUiIT` | Apply for 3-day casual leave | Status PENDING, 3 days, row in "My leave records" |
| | | End date before start date | Error "End date cannot be before start date"; nothing saved |
| | | Empty reason | "Reason is required" |
| 3. Edit & search | `EditAndSearchUiIT` | Edit pending request | New type/days/reason shown, "Leave request updated." |
| | | Keyword + status + type filters | Exactly one matching row; APPROVED filter → no results |
| | | Cancel pending request | Status CANCELLED; Edit button gone |
| 4. Approval | `ApprovalWorkflowUiIT` | HOD approves with comment | APPROVED, dashboard pending −1 / approved +1, faculty sees comment |
| | | HOD rejects without, then with comment | Error first; then REJECTED with comment |
| 5. Access control | `AccessControlUiIT` | Faculty views own record | No review controls |
| | | Faculty opens another's record | "Not found" page; not in list |
| | | HOD list | Sees all records; no "Apply" link |

## 3. Running locally

```bash
mvn verify                       # unit tests + Selenium (HtmlUnit)
mvn verify -Dbrowser=chrome      # same suite in headless Chrome (Chrome must be installed)
mvn verify -Dit.test=ApprovalWorkflowUiIT    # one journey
```

Reports: `target/surefire-reports/` (unit) and `target/failsafe-reports/` (Selenium).

## 4. Result of local run

See `docs/evidence/task09-local-selenium-run.txt`: **24 unit tests and 14 Selenium tests, 0 failures, BUILD SUCCESS.**

## 5. Failure screenshot mechanism

`ScreenshotOnFailure` is a JUnit 5 extension (`AfterTestExecutionCallback`) registered in `BaseUiIT`.
When a test throws, it runs *before* `@AfterEach` closes the browser and saves:

- the page HTML with its URL (all drivers), and
- a PNG screenshot (drivers implementing `TakesScreenshot`, e.g. Chrome/Firefox).

Jenkins archives `target/selenium-screenshots/**` (Task 10). The mechanism was exercised for real in
Task 10 — see `docs/evidence/task10-*`.
