---
title: Operations
description: Build, configure, and validate a ModularJobs Paper deployment.
---

## Release checklist

1. Build the Paper artifact with `./gradlew :paper:build`.
2. Apply `paper/src/main/resources/sql/postgres.sql` out of band.
3. Review generated `config.yml` and `database.yml` before enabling jobs.
4. Install only the optional integrations required by the server.
5. Treat the YAML, JSON, and CSV resources as starter content and replace
   economy values before production use.

## Secure editor checklist

The editor is disabled by default. If enabled, configure an operator-managed
REST session API, web editor URL, create secret, and PostgreSQL session schema.
The Paper plugin does not launch or provision those services.

## Verification

Run the Java module tests from the repository root. For the web stack, run
`cargo test` in `web/rest-api` and `npm test && npm run build` in
`web/session-editor`.
