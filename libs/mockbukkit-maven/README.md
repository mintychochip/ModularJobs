# Vendored MockBukkit (Paper 26.2)

Maven repository for `org.mockbukkit.mockbukkit:mockbukkit-v26.2`.

## Why vendored?

Maven Central only publishes `mockbukkit-v26.1.2` as of this wiring. Paper **26.2** support
lives on MockBukkit branch [`upgrade/v26.2`](https://github.com/MockBukkit/MockBukkit/tree/upgrade/v26.2).

This repo was built from that branch (commit `6c0db68e03e1`) against
`io.papermc.paper:paper-api:26.2.build.65-beta` (branch’s later pin to build 87 does not compile
until MockBukkit implements newer `Entity#setRotation` APIs).

Artifact version: **26.2.0-mj** (local ModularJobs rebuild).

## Rebuild

```bash
git clone --depth 1 --branch upgrade/v26.2 https://github.com/MockBukkit/MockBukkit.git
# pin paper.api.full-version=26.2.build.65-beta, mockbukkit.version=26.2.0-mj
./gradlew publishToMavenLocal -x test -x checkstyleMain -x checkstyleTest -x javadoc
# copy ~/.m2/repository/org/mockbukkit/mockbukkit/mockbukkit-v26.2 into this tree
```

When Central publishes `mockbukkit-v26.2`, drop this directory and resolve from Maven Central.
