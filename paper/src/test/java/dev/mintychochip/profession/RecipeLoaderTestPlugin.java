package dev.mintychochip.profession;

import java.io.InputStream;
import org.bukkit.plugin.java.JavaPlugin;

/** Minimal plugin used to exercise {@code saveResource} + startup recipe loading in tests. */
public class RecipeLoaderTestPlugin extends JavaPlugin {

  @Override
  public void onEnable() {}

  @Override
  public InputStream getResource(String filename) {
    InputStream fromClasspath = RecipeLoaderTestPlugin.class.getClassLoader().getResourceAsStream(filename);
    if (fromClasspath != null) {
      return fromClasspath;
    }
    return super.getResource(filename);
  }
}
