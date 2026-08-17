# Database schema ownership

ModularJobs supports **MySQL 8 only**.

## Rule

| Store | Who creates tables? | Who only connects? |
|-------|---------------------|--------------------|
| **MySQL** | Ops / CI / script **once** | Paper plugin and `web/rest-api` |

The **game process and REST API never run DDL**. That is intentional: multi-instance
servers, least privilege, reviewable migrations, backups, and upgrades do not belong
in `onEnable` or the API request path.

## Source of truth

- `paper/src/main/resources/sql/mysql.sql`

## Provision MySQL

```bash
# Local / CI
export DATABASE_URL=mysql://user:pass@host:3306/modularjobs
./scripts/apply-mysql-schema.sh

# Or, with the MySQL client configured for the target database
mysql --host=host --port=3306 --user=user --password modularjobs \
  < paper/src/main/resources/sql/mysql.sql
```

Then point the plugin at that database:

```yaml
# database.yml
payable:
  type: mysql
  jdbc-url: jdbc:mysql://host:3306/modularjobs
  username: modularjobs
  password: secret
  maximum-pool-size: 10

timed-boost:
  type: mysql
  jdbc-url: jdbc:mysql://host:3306/modularjobs
  username: modularjobs
  password: secret

upgrades:
  type: mysql
  jdbc-url: jdbc:mysql://host:3306/modularjobs
  username: modularjobs
  password: secret
```

Identical `jdbc-url` + `username` sections share one Hikari pool.

## Shared editor session database

The Paper plugin and `web/rest-api` must point at the same MySQL database. The
REST API stores editor payloads in `editor_sessions`; Paper fetches those payloads
through the REST API and applies task changes through its existing repositories.

The plugin does not launch MySQL, manage its process, create a data directory,
or replace operator backups and upgrades. For local development, run MySQL 8
externally (for example, Docker/Podman) and apply the schema before starting either
process.

## Startup behavior

1. Open Hikari pool to MySQL.
2. Verify required tables exist (`job_progression`, `job_tasks`, …).
3. If any table is missing → **fail enable** with a message pointing at
   `scripts/apply-mysql-schema.sh`.

## Job and task configuration updates

The bundled `job_tasks.csv` is the authoritative seed file when the task table is
empty. Startup intentionally skips import when MySQL already contains tasks.
Changing the packaged CSV does not overwrite live task data. Operators must back up
and apply task changes through the existing editor/repository path or an explicit
reviewed SQL operation. Do not clear a live task table without a backup and reimport
plan.

The `fisher` → `fisherman` job-key rename likewise requires an operator-managed data
update for existing progression, upgrade, task, and payable rows before deploying the
renamed catalog.

SQLite, PostgreSQL, and MariaDB are **not supported**.
