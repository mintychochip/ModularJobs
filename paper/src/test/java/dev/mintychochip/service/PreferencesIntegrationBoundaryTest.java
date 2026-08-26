package dev.mintychochip.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Structural checks for the loadable Preferences API boundary. */
class PreferencesIntegrationBoundaryTest {

  @Test
  void integrationLayerDoesNotReferenceExternalApi() throws Exception {
    String integration =
        Files.readString(
            Path.of("src/main/java/dev/mintychochip/service/PreferencesIntegration.java"));
    assertFalse(
        integration.contains("dev.mintychochip.preferences.api"),
        "PreferencesIntegration must not import the external API");
    assertTrue(
        integration.contains("NoClassDefFoundError"),
        "PreferencesIntegration must catch missing API classes");
  }

  @Test
  void payableLayerDoesNotReferenceExternalApi() throws Exception {
    String provider =
        Files.readString(
            Path.of("src/main/java/dev/mintychochip/payable/ExperienceBarColorProvider.java"));
    String wiring =
        Files.readString(Path.of("src/main/java/dev/mintychochip/payable/PayableWiring.java"));
    assertFalse(provider.contains("dev.mintychochip.preferences.api"));
    assertFalse(wiring.contains("dev.mintychochip.preferences.api"));
  }

  @Test
  void externalApiReferencesAreIsolatedToBridge() throws Exception {
    String bridge =
        Files.readString(
            Path.of("src/main/java/dev/mintychochip/service/ExternalPreferencesBridge.java"));
    assertTrue(bridge.contains("dev.mintychochip.preferences.api.PreferencesService"));
  }
}
