package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Static audit of shipped {@code plugin.yml} for production readiness gates.
 */
class PluginYmlProductionReadinessTest {

  @Test
  void pluginYmlHasNoDebugTestCommandAndDeclaresAdminPermissions() throws Exception {
    try (InputStream in = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("plugin.yml")) {
      assertNotNull(in, "plugin.yml must be on the main resources classpath");
      YamlConfiguration yml = YamlConfiguration.loadConfiguration(
          new InputStreamReader(in, StandardCharsets.UTF_8));

      assertFalse(yml.contains("commands.test"),
          "production plugin.yml must not register unrestricted commands.test");
      assertFalse(yml.contains("commands") && yml.getConfigurationSection("commands") != null
              && yml.getConfigurationSection("commands").contains("test"),
          "no commands.test entry");

      String api = yml.getString("api-version");
      assertNotNull(api);
      // Must not be the stale 1.13 stub
      assertFalse("1.13".equals(api), "api-version must be modern (not 1.13), got " + api);

      assertTrue(yml.getStringList("softdepend").stream()
              .anyMatch(s -> s.equalsIgnoreCase("Mint")),
          "softdepend must list Mint");
      assertTrue(yml.getStringList("softdepend").stream()
              .anyMatch(s -> s.equalsIgnoreCase("PlaceholderAPI")),
          "softdepend must list PlaceholderAPI");
      assertFalse(yml.getStringList("softdepend").stream()
              .anyMatch(s -> s.equalsIgnoreCase("Preferences")),
          "Preferences must not remain a runtime soft dependency");

      assertNotNull(yml.getConfigurationSection("permissions.modularjobs.admin"));
      assertNotNull(yml.getConfigurationSection("permissions.jobs.command.browse"));
    }
  }
}
