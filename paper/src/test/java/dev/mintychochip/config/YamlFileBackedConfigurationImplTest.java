package dev.mintychochip.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class YamlFileBackedConfigurationImplTest {

  @BeforeEach
  void setUp() {
    MockBukkit.mock();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  @Test
  void createSucceedsWhenThreadContextClassLoaderCannotSeeYamlConfiguration() throws IOException {
    JavaPlugin plugin = MockBukkit.loadSimple(YamlConfigTestPlugin.class);
    File yaml = new File(plugin.getDataFolder(), "dummy.yml");
    Files.createDirectories(yaml.getParentFile().toPath());
    Files.writeString(yaml.toPath(), "greeting: hello\n");

    ClassLoader previous = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader isolated =
        new URLClassLoader(new java.net.URL[0], ClassLoader.getPlatformClassLoader())) {
      Thread.currentThread().setContextClassLoader(isolated);
      YamlConfiguration config = YamlConfiguration.create(plugin, "dummy.yml");
      assertNotNull(config);
      assertEquals("hello", config.getString("greeting"));
    } finally {
      Thread.currentThread().setContextClassLoader(previous);
    }
  }
}
