# Evidence checklist (screenshots / logs to collect)

Already in the repository (generated while building the project):

- [x] `docs/evidence/task06-merge-conflict.diff` — merge conflict as shown by Git
- [x] `docs/evidence/task09-local-selenium-run.txt` — 24 unit + 14 Selenium tests passing locally
- [x] `docs/evidence/task10-defect-failed-run.txt` — unit tests pass, Selenium gate fails on injected defect
- [x] `docs/evidence/task10-failure-snapshot.html` — page captured by the failure mechanism
- [x] `docs/evidence/task10-fixed-successful-run.txt` — green rerun after the fix
- [x] `git log --oneline --graph --all` — branches, PR merges, conflict merge, tags

To capture on your machine (need GitHub, Docker, Jenkins running):

| Task | Screenshot / log |
|---|---|
| 4 | GitHub repo home (README), Issues list, branch protection rule |
| 5 | Pull request page for `feature/5-apply-leave` with review comment, merged |
| 6 | Running MVP: dashboard, search results, HOD approve screen; `git show v1.0.0` |
| 7 | Freestyle job config; console `Finished: SUCCESS`; "Started by an SCM change"; archived WAR |
| 8 | Pipeline Stage View; console of Deploy + Smoke test; app on Tomcat with *staging* badge |
| 9 | Terminal `mvn verify` output; `target/failsafe-reports` |
| 10 | Red build at Selenium gate (Package/Deploy skipped); Test Result page; green build after fix |
| 11 | `docker build`, `docker images`, `docker ps (healthy)`, `docker logs`, stop/start/rm |
| 12 | Full green Stage View with Docker stages; registry tag list; new container serving new version |
| 13 | `ansible-playbook site.yml` first run with PLAY RECAP |
| 14 | Clean-host proof; rerun `changed=0`; rollback run; `/actuator/info` before/after |
| 15 | Demo video; final tag `v1.1.0` on GitHub; presentation |
