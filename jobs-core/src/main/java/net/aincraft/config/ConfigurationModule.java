package net.aincraft.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public final class ConfigurationModule extends AbstractModule {

  private final Plugin plugin;

  public ConfigurationModule(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  protected void configure() {
    YamlConfiguration configuration = YamlConfiguration.create(plugin, "database.yml");
    bind(YamlConfiguration.class).annotatedWith(Names.named("database"))
        .toInstance(configuration);
  }

  @Provides
  @Singleton
  public ColorScheme colorScheme() {
    YamlConfiguration config = YamlConfiguration.create(plugin, "config.yml");

    if (!config.contains("colors")) {
      return ColorScheme.defaults();
    }

    ConfigurationSection colors = config.getConfigurationSection("colors");

    return new ColorScheme(
        parseColor(colors, "primary", "#FFD700"),
        parseColor(colors, "secondary", "#FFFF00"),
        parseColor(colors, "accent", "#00FFFF"),
        parseColor(colors, "tertiary", "#FF00FF"),
        parseColor(colors, "neutral", "#808080"),
        parseColor(colors, "error", "#FF0000")
    );
  }

  private TextColor parseColor(ConfigurationSection section, String key, String defaultHex) {
    String hex = section != null ? section.getString(key, defaultHex) : defaultHex;
    return TextColor.fromHexString(hex);
  }
}
