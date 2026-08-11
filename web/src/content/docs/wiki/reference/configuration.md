---
title: Configuration reference
description: Current ModularJobs operator configuration and distribution defaults.
---

## Economy

```yaml
economy:
  required: false
  missing-provider: blackhole # blackhole | fail
```

Mint is optional. `blackhole` is the safe default when no provider is
available; positive economy payables are accepted and discarded. Use `fail`
when currency rewards are mandatory. Legacy `required: true` maps to `fail`
when no explicit policy is present.

## Editor

```yaml
editor:
  enabled: false
  session-api-url: ""
  web-editor-url: ""
  session-create-secret: ""
  session-ttl-minutes: 1440
```

The editor requires an external REST API and web application. Configure both
URLs and the create secret explicitly before enabling it.

## Persistence

Paper and the REST API use operator-managed PostgreSQL. Apply schema files
before startup; neither process creates tables during normal operation.
