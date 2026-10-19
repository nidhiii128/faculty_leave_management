# Task 14 — Automated Provisioning and Reliability Validation

## 1. Clean target environment

Pick one:

| Option | How |
|---|---|
| **Vagrant VM** (recommended) | `cd ansible && vagrant up` → fresh Ubuntu 22.04 at 192.168.56.20; uncomment the `vm` line in `inventory.ini` |
| Fresh WSL distro | PowerShell: `wsl --install -d Ubuntu-22.04 --name flms-target` (or `wsl --import`), install Ansible inside it, use the `local` host |
| Cloud VM | Any Ubuntu 22.04 instance; add it to `[app]` with its IP and SSH user |

Prove the host is clean first: `docker --version` → *command not found*, `id flms` → *no such user*.

The image must be in a registry the target can reach (Task 12 pushes `localhost:5000/flms` or Docker Hub).
For a VM, use Docker Hub or set `app_image: <jenkins-host-ip>:5000/flms` (and allow that insecure registry).

## 2. Provision and deploy

```bash
cd ansible
ansible-playbook site.yml -l vm -e app_version=1.1.0-16 | tee ../docs/evidence/task14-1-provision.log
```

Then deploy a newer release (this records 1.1.0-16 as the rollback target):

```bash
ansible-playbook site.yml -l vm -e app_version=1.1.0-17 | tee ../docs/evidence/task14-2-upgrade.log
```

## 3. Idempotency

Run the **same** command again without changing anything:

```bash
ansible-playbook site.yml -l vm -e app_version=1.1.0-17 | tee ../docs/evidence/task14-3-idempotent.log
```

Expected `PLAY RECAP`: `ok=N  changed=0  failed=0`. Why it holds:

- `apt`, `user`, `file`, `template`, `copy` only act if the actual state differs from the declared state.
- `docker_container` only recreates the container when the image, ports, volumes, env file or user differ.
- `uri` health check and `docker info` are read-only (`changed_when: false`).
- `previous_version` is written only when the version actually changes.

## 4. Health check

Built into every run (`roles/flms/tasks/main.yml`):

```yaml
- name: Health check — wait until the application reports UP
  ansible.builtin.uri:
    url: "{{ health_url }}"            # http://localhost:8090/actuator/health
    return_content: true
  register: flms_health
  until: flms_health.status == 200 and '"UP"' in flms_health.content
  retries: 30
  delay: 4
```

A release that does not start fails the playbook (`failed=1`), so a broken deployment is visible
immediately. Manual checks: `curl http://<host>:8090/actuator/health`, `docker ps` (`healthy`),
`curl http://<host>:8090/actuator/info` (version + environment).

## 5. Rollback / recovery

```bash
cat /opt/flms/current_version /opt/flms/previous_version     # 1.1.0-17 / 1.1.0-16
ansible-playbook rollback.yml -l vm | tee ../docs/evidence/task14-4-rollback.log
curl http://<host>:8090/actuator/info                          # version from 1.1.0-16 image
```

`rollback.yml` reads `previous_version`, redeploys that tag with the same roles (so the health check runs
again) and swaps the version records — running it twice rolls forward again.
Data survives because the database lives in `/var/lib/flms` on the host, not in the container.

**Recovery drill** (container crash): `docker kill flms` → Docker restarts it (`unless-stopped`);
or `docker rm -f flms` → rerun `site.yml` → container recreated, `changed=1` for that task only.

Rollback timing: the image is already on the host, so rollback takes ≈ the application start time
(~20–40 s), within the < 2 min target (success criterion S7).

## 6. Evidence to capture

- Clean-host proof, then the four logs above (`task14-1..4`).
- The `PLAY RECAP` lines: first run `changed>0`, rerun `changed=0`.
- `docker ps` + `/actuator/info` before and after rollback.
