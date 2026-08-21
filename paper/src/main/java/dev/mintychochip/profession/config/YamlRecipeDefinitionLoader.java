package dev.mintychochip.profession.config;

import dev.mintychochip.profession.ProfessionCatalog;
import dev.mintychochip.profession.RecipeDefinition;
import dev.mintychochip.service.RecipeService;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Loads {@link RecipeDefinition} records from {@code recipes.yml} into {@link RecipeService}.
 *
 * <p>Operators edit the data-folder copy; bundled starter entries cover common blacksmith craft
 * outputs so recipe gates and experience depreciation are active out of the box.
 */
public final class YamlRecipeDefinitionLoader {

  private static final String CONFIG_FILE = "recipes.yml";
  private static final String RECIPES_SECTION = "recipes";

  private YamlRecipeDefinitionLoader() {}

  /**
   * Ensures {@code recipes.yml} exists, parses it, and registers every valid definition.
   *
   * @return number of definitions registered
   */
  public static int load(@NotNull Plugin plugin, @NotNull RecipeService recipeService) {
    if (plugin instanceof org.bukkit.plugin.java.JavaPlugin javaPlugin) {
      javaPlugin.saveResource(CONFIG_FILE, false);
    } else {
      throw new IllegalArgumentException("recipe loader requires a JavaPlugin for saveResource");
    }
    return loadFromDataFolder(plugin.getDataFolder(), recipeService, plugin.getLogger());
  }

  /** Loads {@code recipes.yml} from an on-disk plugin data folder (post {@code saveResource}). */
  static int loadFromDataFolder(
      @NotNull File dataFolder, @NotNull RecipeService recipeService, @NotNull Logger logger) {
    File configFile = new File(dataFolder, CONFIG_FILE);
    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    return loadFromConfiguration(config, recipeService, logger);
  }

  /**
   * Parses the {@code recipes} section and registers each definition.
   *
   * @throws IllegalArgumentException when a recipe entry is invalid
   */
  public static int loadFromConfiguration(
      @NotNull ConfigurationSection config,
      @NotNull RecipeService recipeService,
      @NotNull Logger logger) {
    ConfigurationSection recipes = config.getConfigurationSection(RECIPES_SECTION);
    if (recipes == null || recipes.getKeys(false).isEmpty()) {
      logger.info(
          "No recipe definitions in " + CONFIG_FILE + "; craft gates and depreciation stay inert");
      return 0;
    }

    List<RecipeDefinition> parsed = parseAll(recipes);
    for (RecipeDefinition definition : parsed) {
      recipeService.registerDefinition(definition);
    }
    logger.info("Registered " + parsed.size() + " recipe definition(s) from " + CONFIG_FILE);
    return parsed.size();
  }

  static @NotNull List<RecipeDefinition> parseAll(@NotNull ConfigurationSection recipesSection) {
    Set<String> recipeKeys = recipesSection.getKeys(false);
    List<RecipeDefinition> parsed = new ArrayList<>(recipeKeys.size());
    Map<Key, String> craftOutputOwners = new HashMap<>();
    for (String recipeKey : recipeKeys) {
      ConfigurationSection entry = recipesSection.getConfigurationSection(recipeKey);
      if (entry == null) {
        throw new IllegalArgumentException(
            RECIPES_SECTION + "." + recipeKey + " must be a mapping");
      }
      RecipeDefinition definition = parseDefinition(recipeKey, entry);
      String previousOwner = craftOutputOwners.put(definition.craftOutputKey(), recipeKey);
      if (previousOwner != null) {
        throw new IllegalArgumentException(
            "duplicate craft output "
                + definition.craftOutputKey()
                + " for recipes "
                + previousOwner
                + " and "
                + recipeKey);
      }
      parsed.add(definition);
    }
    return parsed;
  }

  static @NotNull RecipeDefinition parseDefinition(
      @NotNull String recipeKey, @NotNull ConfigurationSection entry) {
    Key id = parseKey(recipeKey, "recipe id");

    String professionRaw = requiredString(entry, "profession");
    String professionId =
        ProfessionCatalog.resolve(professionRaw)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "unknown profession for recipe " + recipeKey + ": " + professionRaw))
            .id();

    int requiredLevel = entry.getInt("required-level", entry.getInt("level", 0));
    if (requiredLevel < 1) {
      throw new IllegalArgumentException(
          recipeKey + " must set required-level (or legacy level) >= 1");
    }

    int tier = entry.getInt("tier", 0);
    if (tier < 1 || tier > 5) {
      throw new IllegalArgumentException(recipeKey + " tier must be 1–5");
    }

    String outputRaw = entry.getString("output");
    if (outputRaw == null || outputRaw.isBlank()) {
      return new RecipeDefinition(id, professionId, requiredLevel, tier);
    }
    Key outputKey = parseKey(outputRaw, "output");
    return new RecipeDefinition(id, professionId, requiredLevel, tier, outputKey);
  }

  private static @NotNull String requiredString(ConfigurationSection entry, String field) {
    String value = entry.getString(field);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("missing " + field);
    }
    return value.trim();
  }

  private static @NotNull Key parseKey(String raw, String label) {
    String normalized = raw.trim().toLowerCase(Locale.ROOT);
    try {
      return Key.key(normalized);
    } catch (InvalidKeyException failure) {
      throw new IllegalArgumentException("invalid " + label + " key: " + raw, failure);
    }
  }
}
