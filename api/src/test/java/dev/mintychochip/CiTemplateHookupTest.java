package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class CiTemplateHookupTest {
  private static final String RELEASE_VERSION = "26.8.17.99";

  @Test
  void consumerWorkflowBuildsAndUploadsApiArtifact() throws IOException {
    Path workflow = Path.of(requiredProperty("ci.workflow"));
    String text = Files.readString(workflow);
    assertTrue(
        text.contains("name: API build + publish artifact"),
        "missing inline api-build job in " + workflow);
    assertTrue(
        text.contains(":api:test :api:build"),
        "missing api gradle tasks in " + workflow);
    assertTrue(
        text.contains("api/build/libs/*.jar"),
        "missing api jar upload glob in " + workflow);
    assertFalse(
        text.contains("aincraft-org/ci-template/.github/workflows/paper.yml@"),
        "reusable workflow must not be referenced from " + workflow
            + " (aincraft-org/ci-template is private and unresolvable from a public repo)");
  }

  @Test
  void ciUsesPinnedActionMajorsAndCleanCheck() throws IOException {
    String text = Files.readString(Path.of(requiredProperty("ci.workflow")));
    assertTrue(text.contains("actions/checkout@v7"), "checkout must be @v7");
    assertTrue(text.contains("actions/setup-java@v5"), "setup-java must be @v5");
    assertTrue(text.contains("gradle/actions/setup-gradle@v6"), "setup-gradle must be @v6");
    assertTrue(text.contains("./gradlew clean check"), "java gate must be clean check");
    assertFalse(
        text.contains("java-version: \"21\""),
        "CI must not install JDK 21 after the Java 25-only cutover");
  }

  @Test
  void nightlyIsScheduleOrManualAndPublishesShadowJar() throws IOException {
    Path nightly =
        Path.of(requiredProperty("project.root")).resolve(".github/workflows/nightly.yml");
    assertTrue(Files.isRegularFile(nightly), "missing " + nightly);
    String text = Files.readString(nightly);
    assertTrue(text.contains("cron: '0 4 * * *'"), "nightly must schedule at 04:00 UTC");
    assertTrue(text.contains("workflow_dispatch"), "nightly must be manually dispatchable");
    assertTrue(text.contains("gh release create nightly"), "must replace rolling nightly tag");
    assertTrue(text.contains("paper/build/libs/*-all.jar"), "must publish paper shadow jar");
    assertTrue(isScheduleOrManualOnly(text), "nightly must not run on push/PR/tag");
  }

  @Test
  void leftoverLocalJobsDoNotPublishOnPushPrOrTag() throws IOException {
    Path dir = Path.of(requiredProperty("project.root")).resolve(".github/workflows");
    try (var stream = Files.list(dir)) {
      for (Path file : stream.filter(path -> path.toString().endsWith(".yml")).toList()) {
        String text = Files.readString(file);
        if (!publishesPackagesOrRelease(text)) {
          continue;
        }
        assertTrue(
            isScheduleOrManualOnly(text),
            "leftover Packages/Release on push/PR/tag in " + file);
      }
    }
  }

  @Test
  void releaseVersionBecomesPublishedPomVersion() throws Exception {
    Path root = Path.of(requiredProperty("project.root"));
    ProcessBuilder builder =
        new ProcessBuilder(
            root.resolve("gradlew").toAbsolutePath().toString(),
            "--no-daemon",
            "-q",
            ":api:generatePomFileForMavenPublication",
            "-PreleaseVersion=" + RELEASE_VERSION);
    builder.directory(root.toFile());
    builder.redirectErrorStream(true);
    Process process = builder.start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(process.waitFor(5, TimeUnit.MINUTES), "gradlew timed out");
    assertEquals(0, process.exitValue(), output);
    String pom = Files.readString(Path.of(requiredProperty("ci.pom")));
    assertTrue(pom.contains("<version>" + RELEASE_VERSION + "</version>"), pom);
    String build = Files.readString(root.resolve("build.gradle.kts"));
    assertTrue(build.contains("https://maven.pkg.github.com/aincraft-org/modularjobs"), build);
    assertTrue(build.contains("GITHUB_ACTOR"), build);
    assertTrue(build.contains("GITHUB_TOKEN"), build);
  }

  private static boolean publishesPackagesOrRelease(String text) {
    return text.contains("ToGitHubPackages")
        || text.contains("softprops/action-gh-release")
        || text.contains("gh release create");
  }

  private static boolean isScheduleOrManualOnly(String text) {
    return !text.contains("\n  push:")
        && !text.contains("\n  pull_request:")
        && !text.contains("\n  tags:");
  }

  private static String requiredProperty(String name) {
    String value = System.getProperty(name);
    assertTrue(value != null && !value.isBlank(), "missing system property " + name);
    return value;
  }
}
