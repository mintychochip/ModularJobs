package net.aincraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  void consumerWorkflowUsesReusablePaperWorkflow() throws IOException {
    Path workflow = Path.of(requiredProperty("ci.workflow"));
    String text = Files.readString(workflow);
    assertTrue(
        text.contains("aincraft-org/ci-template/.github/workflows/paper.yml@"),
        "missing paper.yml uses in " + workflow);
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
