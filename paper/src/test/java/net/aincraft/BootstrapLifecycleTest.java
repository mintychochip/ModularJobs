package net.aincraft;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Structural proof of disable lifecycle and profession service gating in shipped sources.
 */
class BootstrapLifecycleTest {

  @Test
  void onDisableUnregistersServicesAndPapiAndDoesNotBusySpin() throws Exception {
    Path bootstrap = locate("ModularJobsBootstrap.java");
    String text = Files.readString(bootstrap, StandardCharsets.UTF_8);
    assertTrue(text.contains("Bridge.register"), "must register static Bridge holder on enable");
    assertTrue(text.contains("Bridge.unregister"), "must clear static Bridge holder on disable");
    assertTrue(text.contains("PluginProvider.set"), "must set/clear paper PluginProvider");
    assertTrue(text.contains("unregisterAll"), "must unregister Bukkit services on disable");
    assertTrue(text.contains("unregister()"), "must unregister PlaceholderAPI expansion");
    assertTrue(text.contains("ctx.shutdown()"), "must flush/close via PluginContext.shutdown");
    assertFalse(
        text.contains("onSpinWait"),
        "bootstrap must not busy-spin; flush wait lives in write-back with sleep/timeout");
  }

  @Test
  void professionServicesGatedByConfig() throws Exception {
    Path bootstrap = locate("ModularJobsBootstrap.java");
    String text = Files.readString(bootstrap, StandardCharsets.UTF_8);
    assertTrue(
        text.contains("profession-apis.register-bukkit-services"),
        "profession Bukkit services must be feature-flagged");
  }

  @Test
  void pluginContextWiresLocalPreferencesService() throws Exception {
    Path context = locate("PluginContext.java");
    String text = Files.readString(context, StandardCharsets.UTF_8);
    assertTrue(
        text.contains("new PreferencesServiceImpl(plugin)"),
        "PluginContext must construct the always-available local preference service");
    assertFalse(
        text.contains("PreferencesIntegration"),
        "PluginContext must not depend on the removed external Preferences adapter");
    assertFalse(
        text.contains("ExternalBackedPreferencesService"),
        "PluginContext must not depend on the removed external facade");
  }

  @Test
  void progressionWriteBackFlushPendingUsesSleepNotSpinWait() throws Exception {
    Path writeBack = locate("domain/WriteBackJobProgressionRepositoryImpl.java");
    String text = Files.readString(writeBack, StandardCharsets.UTF_8);
    assertTrue(text.contains("Thread.sleep"), "flushPending must sleep while waiting for lock");
    assertFalse(text.contains("onSpinWait"), "flushPending must not busy-spin");
    assertTrue(text.contains("preferHigherExperience") || text.contains("merge("),
        "flush failure re-queue must merge XP safely");
    assertTrue(text.contains("key.jobKey()"), "loadAllForJob must compare job key");
  }

  @Test
  void timedBoostWriteBackFlushPendingUsesSleepNotSpinWait() throws Exception {
    Path writeBack = locate("repository/WriteBackRepositoryImpl.java");
    String text = Files.readString(writeBack, StandardCharsets.UTF_8);
    assertTrue(
        text.contains("void flushPending()"),
        "WriteBackRepositoryImpl must expose flushPending for disable path");
    assertTrue(
        text.contains("Thread.sleep"),
        "WriteBackRepositoryImpl.flushPending must sleep while waiting for lock");
    assertFalse(
        text.contains("onSpinWait"),
        "WriteBackRepositoryImpl.flushPending must not busy-spin (timed-boost disable hang)");
    assertTrue(
        text.contains("FLUSH_LOCK_WAIT_MS") || text.contains("Timed out waiting"),
        "WriteBackRepositoryImpl.flushPending must timeout instead of hang forever");
  }

  private static Path locate(String relativeUnderAincraft) {
    Path root = Path.of("").toAbsolutePath();
    Path candidate =
        root.resolve("paper/src/main/java/net/aincraft/" + relativeUnderAincraft);
    if (Files.isRegularFile(candidate)) {
      return candidate;
    }
    candidate = root.resolve("src/main/java/net/aincraft/" + relativeUnderAincraft);
    if (Files.isRegularFile(candidate)) {
      return candidate;
    }
    throw new IllegalStateException(
        "Cannot find " + relativeUnderAincraft + " from " + root);
  }
}
