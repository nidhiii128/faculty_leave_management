# Task 8 — Pipeline as Code and Server Deployment

## 1. Jenkinsfile

`Jenkinsfile` in the repository root is a declarative pipeline:

| Stage | What it does |
|---|---|
| Checkout | `checkout scm`, prints the commit |
| Build | `mvn clean compile -Dapp.env=<APP_ENV>` |
| Unit tests | `mvn test`; publishes JUnit results |
| Package | `mvn package`; archives `target/flms.war` with fingerprint |
| Deploy to Tomcat | Uploads the WAR to the Tomcat **manager text API** at context `/flms` (`update=true` redeploys) |
| Smoke test | Polls `/flms/actuator/health` until `"UP"`, prints `/actuator/info` |

(Tasks 10 and 12 add the Selenium quality gate and Docker stages to the same file.)

## 2. Parameterised environment setting

| Parameter | Values | Effect |
|---|---|---|
| `APP_ENV` | dev / staging / prod | Passed as `-Dapp.env=...`; Maven resource filtering writes it into `application.properties` inside the WAR → shown in the app header badge and `/actuator/info` |
| `DEPLOY_TARGET` | tomcat / none | Skip deployment for CI-only runs |
| `TOMCAT_URL` | URL | Deployment server |

Verified locally:

```bash
mvn -B package -DskipTests -Dapp.env=staging
unzip -p target/flms.war WEB-INF/classes/application.properties | grep app.environment
# app.environment=${APP_ENV:staging}
```

The runtime variable `APP_ENV` still overrides the baked-in value (used by Docker/Ansible).

## 3. Tomcat deployment target

`jenkins/docker-compose.yml` starts **Tomcat 10.1** next to Jenkins with the manager app enabled
(`jenkins/tomcat/tomcat-users.xml`, user `deployer` with role `manager-script`).

The WAR is built with `SpringBootServletInitializer`, so the *same* file runs in external Tomcat
and standalone (`java -jar flms.war`).

## 4. Create the pipeline job

1. *Manage Jenkins → Credentials → System → Global → Add*: Kind **Username with password**,
   ID `tomcat-manager`, username `deployer`, password as in `tomcat-users.xml`.
2. *New Item → `flms-pipeline` → Pipeline*.
3. *Pipeline → Definition: Pipeline script from SCM*, SCM Git, repo URL, branch `*/develop`,
   Script Path `Jenkinsfile`.
4. Save → **Build Now** once (Jenkins reads the parameters) → **Build with Parameters**, `APP_ENV=staging`.
5. Open http://localhost:8888/flms → the header badge shows **staging · v1.x**.

Without Docker, use a native Tomcat 10: copy `jenkins/tomcat/tomcat-users.xml` to `$CATALINA_HOME/conf/`,
restart Tomcat and set `TOMCAT_URL=http://localhost:8080`.

## 5. Evidence to capture

- `Jenkinsfile` in the repo; *Stage View* with all stages green.
- Console output of *Deploy to Tomcat* (`OK - Deployed application at context path [/flms]`)
  and *Smoke test* (`Application is UP`).
- Browser screenshot of `http://localhost:8888/flms` with the environment badge; Tomcat manager list showing `/flms`.
