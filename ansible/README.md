# Ansible — provisioning and deployment

| File | Purpose |
|---|---|
| `inventory.ini` | Target nodes (`local` or the Vagrant `vm`) |
| `group_vars/all.yml` | Configuration specification: user, folders, ports, packages, image, version |
| `roles/common` | Packages, user/group, directories, environment file |
| `roles/docker` | Docker Engine installed, running, enabled |
| `roles/flms` | Pull image, run container, health check, record current/previous version |
| `site.yml` | Full provisioning + deployment |
| `rollback.yml` | Redeploy the previously running version |
| `Vagrantfile` | Clean Ubuntu VM target |

```bash
cd ansible
ansible-galaxy collection install -r requirements.yml
ansible-playbook site.yml --syntax-check
ansible-playbook site.yml -K -e app_version=1.1.0-17     # deploy
ansible-playbook site.yml -K -e app_version=1.1.0-17     # rerun: changed=0
ansible-playbook rollback.yml -K                         # back to the previous version
```

Details: `docs/13-configuration-management.md`, `docs/14-provisioning-reliability.md`.
