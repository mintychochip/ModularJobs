package net.aincraft.boost.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.aincraft.container.BoostSource;
import net.aincraft.container.boost.factories.BoostFactory;
import net.aincraft.container.boost.factories.ConditionFactory;
import net.aincraft.container.boost.factories.PolicyFactory;
import net.aincraft.registry.Registry;
import org.bukkit.plugin.Plugin;

/**
 * Loads boost sources from JSON configuration file.
 */
public final class BoostSourceLoader {

  private static final String CONFIG_FILE = "boost_sources.json";
  private static final String DEFAULT_RESOURCE = "boost_sources_default.json";

  private final Plugin plugin;
  private final Gson gson;
  private final BoostSourceConfigParser parser;
  private final Registry<BoostSource> registry;

  public BoostSourceLoader(
      Plugin plugin,
      Gson gson,
      ConditionFactory conditionFactory,
      PolicyFactory policyFactory,
      BoostFactory boostFactory,
      Registry<BoostSource> registry
  ) {
    this.plugin = plugin;
    this.gson = gson;
    this.parser = new BoostSourceConfigParser(conditionFactory, policyFactory, boostFactory);
    this.registry = registry;
  }

  /**
   * Load boost sources from configuration file.
   * Creates default config if it doesn't exist.
   *
   * @return number of boost sources loaded
   */
  public int load() {
    File configFile = new File(plugin.getDataFolder(), CONFIG_FILE);

    // Create default config if it doesn't exist
    if (!configFile.exists()) {
      createDefaultConfig(configFile);
    }

    try (FileReader reader = new FileReader(configFile)) {
      return loadFromReader(reader);
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to load boost sources: " + e.getMessage());
      return 0;
    }
  }

  /**
   * Reload boost sources from configuration file.
   *
   * @return number of boost sources loaded
   */
  public int reload() {
    // Note: We don't clear the registry since other boost sources might be registered
    // This just re-registers/overwrites existing ones
    return load();
  }

  private int loadFromReader(java.io.Reader reader) {
    JsonObject root = gson.fromJson(reader, JsonObject.class);
    if (root == null || !root.has("boost_sources")) {
      plugin.getLogger().warning("No 'boost_sources' key found in configuration");
      return 0;
    }

    JsonObject boostSources = root.getAsJsonObject("boost_sources");
    int count = 0;

    for (Map.Entry<String, com.google.gson.JsonElement> entry : boostSources.entrySet()) {
      try {
        BoostSourceConfig config = gson.fromJson(entry.getValue(), BoostSourceConfig.class);
        BoostSource boostSource = parser.parse(config);
        registry.register(boostSource);
        count++;
      } catch (Exception e) {
        plugin.getLogger().warning(
            "Failed to parse boost source '" + entry.getKey() + "': " + e.getMessage()
        );
      }
    }

    plugin.getLogger().info("Loaded " + count + " boost source(s)");
    return count;
  }

  private void createDefaultConfig(File configFile) {
    try {
      // Ensure data folder exists
      if (!plugin.getDataFolder().exists()) {
        plugin.getDataFolder().mkdirs();
      }

      // Try to copy from resources
      try (InputStream resource = plugin.getResource(DEFAULT_RESOURCE)) {
        if (resource != null) {
          Files.copy(resource, configFile.toPath());
          plugin.getLogger().info("Created default boost sources configuration");
          return;
        }
      }

      // If no default resource, create minimal config
      String defaultJson = """
          {
            "boost_sources": {
              "example_boost": {
                "key": "modularjobs:example_boost",
                "policy": {
                  "type": "all_applicable"
                },
                "rules": [
                  {
                    "priority": 10,
                    "conditions": {
                      "type": "always"
                    },
                    "boost": {
                      "type": "multiplicative",
                      "amount": 1.5
                    }
                  }
                ]
              }
            }
          }
          """;
      Files.writeString(configFile.toPath(), defaultJson);
      plugin.getLogger().info("Created minimal boost sources configuration");
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to create default config: " + e.getMessage());
    }
  }
}
