# Task 7 — Jenkins Installation and Continuous Integration Job

## 1. Install Jenkins

### Option A — Docker (recommended; also gives a Tomcat for Task 8)

Prerequisite: Docker Desktop with WSL 2 integration enabled (Docker Desktop → Settings → Resources →
WSL integration → enable for Ubuntu), or Docker Engine on Linux.

```bash
cd jenkins
docker compose up -d --build          # builds jenkins/Dockerfile: JDK 17 + Maven + Docker CLI + plugins
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Open http://localhost:8081, paste the password, choose **Install suggested plugins**
(the project's plugins from `jenkins/plugins.txt` are already installed), create the admin user.

### Option B — Native install on Ubuntu / WSL

```bash
sudo apt update && sudo apt install -y openjdk-17-jdk maven git curl
sudo wget -O /usr/share/keyrings/jenkins-keyring.asc https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
echo "deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" \
  | sudo tee /etc/apt/sources.list.d/jenkins.list
sudo apt update && sudo apt install -y jenkins
sudo systemctl enable --now jenkins
sudo cat /var/lib/jenkins/secrets/initialAdminPassword      # then open http://localhost:8080
```

### Configure tools

*Manage Jenkins → Tools*: the Docker image already has JDK 17 and Maven on the PATH. On a native install,
either install them with apt (above) or add **JDK** and **Maven** installations there.

## 2. Freestyle CI job (Task 7 deliverable)

*New Item → `flms-ci` → Freestyle project*

| Section | Setting |
|---|---|
| Source Code Management | Git, URL `https://github.com/<user>/faculty-leave-devops.git`, branch `*/develop` |
| Build Triggers | **Poll SCM**, schedule `H/5 * * * *` (or *GitHub hook trigger* when using a webhook) |
| Build Steps | *Invoke top-level Maven targets* → Goals: `-B clean package` |
| Post-build Actions | *Publish JUnit test result report* → `target/surefire-reports/*.xml` |
| | *Archive the artifacts* → `target/flms.war`, tick *Fingerprint* |

Save → **Build Now**.

### Trigger evidence

1. Make a small commit and push it to `develop`:
   ```bash
   git commit --allow-empty -m "ci: trigger Jenkins build" && git push
   ```
2. Within ~5 minutes a new build starts; its page says **"Started by an SCM change"**.
3. *Polling Log* on the job shows the detected commit.

## 3. Evidence to capture

- Job configuration page (SCM, trigger, build step, post-build).
- Console output ending in `BUILD SUCCESS` / `Finished: SUCCESS`.
- Build page showing "Started by an SCM change" and the archived `flms.war` artifact.
- Test Result trend (24 unit tests passing).
