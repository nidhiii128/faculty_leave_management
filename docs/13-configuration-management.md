# Task 13 — Configuration Management Script (Ansible)

Ansible was chosen over Puppet: agentless (only SSH + Python on the target), YAML playbooks,
and it runs directly from WSL/Linux.

## 1. Configuration specification — server prerequisites

| Category | Item | Value | Managed by |
|---|---|---|---|
| **Packages** | ca-certificates, curl, tzdata | present | `common` |
| | python3-docker | present (needed by `community.docker` modules) | `common` |
| | docker.io (Docker Engine) | present | `docker` |
| **Users / groups** | group `flms` | system group | `common` |
| | user `flms` | system user, shell `/usr/sbin/nologin`, home `/opt/flms` | `common` |
| **Folders** | `/opt/flms` | 0755 flms:flms — config + version records | `common` |
| | `/var/lib/flms` | 0750 flms:flms — H2 database (mounted at `/app/data`) | `common` |
| | `/var/log/flms` | 0750 flms:flms | `common` |
| **Files** | `/opt/flms/flms.env` | 0640 root:flms — `APP_ENV`, `DB_URL`, `SEED_DATA`, `JAVA_OPTS` (from template) | `common` |
| | `/opt/flms/current_version` | deployed image tag | `flms` |
| | `/opt/flms/previous_version` | tag that was replaced (rollback target) | `flms` |
| **Ports** | 8090/tcp (host) → 8080 (container) | `app_port` | `flms` |
| **Services** | `docker` | running, enabled at boot | `docker` |
| | container `flms` | running, `restart: unless-stopped`, runs as uid of `flms` | `flms` |
| **Health** | `GET /actuator/health` | `"UP"` within 30 × 4 s | `flms` |

All values live in `ansible/group_vars/all.yml`, so the same playbook serves dev/staging/prod by
overriding variables (`-e app_env=prod -e app_port=80`).

## 2. Inventory

```ini
[app]
local ansible_connection=local
# vm ansible_host=192.168.56.20 ansible_user=vagrant ansible_ssh_private_key_file=...
```

## 3. Playbook structure

```
ansible/
├── ansible.cfg
├── inventory.ini
├── requirements.yml          community.docker, community.general
├── group_vars/all.yml        the specification above
├── site.yml                  hosts: app, become: true, roles: common → docker → flms
├── rollback.yml              redeploys previous_version
└── roles/
    ├── common/   tasks/main.yml, templates/flms.env.j2
    ├── docker/   tasks/main.yml
    └── flms/     tasks/main.yml
```

Every task uses a declarative module (`apt`, `user`, `file`, `template`, `service`,
`docker_image`, `docker_container`, `uri`, `copy`) rather than shell commands, which is what makes
the playbook idempotent. The only `command` (`docker info`) is marked `changed_when: false`.

## 4. First execution

```bash
sudo apt install -y ansible           # or: pipx install ansible-core
cd ansible
ansible-galaxy collection install -r requirements.yml
ansible-playbook site.yml --syntax-check
ansible-playbook site.yml -K -e app_version=1.1.0-17 | tee ../docs/evidence/task13-first-run.log
```

Expected first run: `changed` > 0 (packages, user, directories, env file, image, container, version files),
`failed=0`, and the final message `flms 1.1.0-17 (staging) is UP on port 8090`.

## 5. Static validation done

```
$ ansible-playbook site.yml --syntax-check       -> playbook: site.yml
$ ansible-playbook rollback.yml --syntax-check   -> playbook: rollback.yml
$ ansible-lint site.yml rollback.yml
Passed: 0 failure(s), 0 warning(s) in 5 files processed. Last profile that met the validation criteria was 'production'.
```
