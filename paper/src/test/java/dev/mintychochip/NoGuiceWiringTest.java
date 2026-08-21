package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.Gson;
import dev.mintychochip.boost.BoostFactoryImpl;
import dev.mintychochip.container.boost.factories.BoostFactory;
import dev.mintychochip.editor.json.GsonProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Proves Guice was removed and manual composition entry points still work. */
class NoGuiceWiringTest {

  @Test
  void sourceTreeContainsNoGuiceImportsOrAnnotations() throws IOException {
    Path root = locateJobsCoreMainJava();
    assertTrue(Files.isDirectory(root), "paper main java dir: " + root);

    List<String> offenders = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(p -> p.toString().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  String text = Files.readString(path);
                  if (text.contains("com.google.inject")
                      || text.contains("javax.inject")
                      || text.contains("@Inject")
                      || text.contains("AbstractModule")
                      || text.contains("Guice.createInjector")) {
                    offenders.add(root.relativize(path).toString());
                  }
                } catch (IOException e) {
                  fail("Failed reading " + path + ": " + e.getMessage());
                }
              });
    }

    assertTrue(offenders.isEmpty(), "Guice remnants remain in: " + offenders);
  }

  @Test
  void bootstrapUsesPluginContextNotGuice() throws IOException {
    Path bootstrap = locateJobsCoreMainJava().resolve("dev/mintychochip/ModularJobsBootstrap.java");
    assertTrue(Files.isRegularFile(bootstrap), bootstrap.toString());
    String text = Files.readString(bootstrap);
    assertFalse(text.contains("Guice"));
    assertFalse(text.contains("Injector"));
    assertFalse(text.contains("PluginModule"));
    assertTrue(
        text.contains("PluginContext.create"), "Bootstrap must use PluginContext composition root");
  }

  @Test
  void pluginContextSourceIsCompositionRoot() throws IOException {
    Path context = locateJobsCoreMainJava().resolve("dev/mintychochip/PluginContext.java");
    assertTrue(Files.isRegularFile(context), "PluginContext.java must exist");
    String text = Files.readString(context);
    assertTrue(
        text.contains("public static PluginContext create"),
        "PluginContext must expose create(...) composition entry point");
    assertFalse(text.contains("com.google.inject"));
    assertFalse(text.contains("javax.inject"));
    assertFalse(text.contains("Injector"));
    assertFalse(text.contains("AbstractModule"));
  }

  @Test
  void pluginContextWiresProfessionApis() throws IOException {
    Path context = locateJobsCoreMainJava().resolve("dev/mintychochip/PluginContext.java");
    String text = Files.readString(context);
    assertTrue(
        text.contains("ProfessionWiring"),
        "PluginContext must compose ProfessionWiring for P6 APIs");
    assertTrue(
        text.contains("ProfessionWiring.create(plugin"),
        "PluginContext must pass plugin so recipe definitions load from recipes.yml");
    assertTrue(
        text.contains("professionService"), "Bridge construction must receive professionService");
    assertTrue(text.contains("recipeService"));
    assertTrue(text.contains("buffService"));
  }

  @Test
  void professionWiringLoadsRecipeDefinitions() throws IOException {
    Path wiring =
        locateJobsCoreMainJava().resolve("dev/mintychochip/profession/ProfessionWiring.java");
    String wiringText = Files.readString(wiring);
    assertTrue(
        wiringText.contains("YamlRecipeDefinitionLoader.load"),
        "ProfessionWiring must load recipe definitions at startup");

    Path loader =
        locateJobsCoreMainJava()
            .resolve("dev/mintychochip/profession/config/YamlRecipeDefinitionLoader.java");
    String loaderText = Files.readString(loader);
    assertTrue(loaderText.contains("recipes.yml"), "Recipe loader must reference recipes.yml");
  }

  @Test
  void bootstrapRegistersProfessionBukkitServices() throws IOException {
    Path bootstrap = locateJobsCoreMainJava().resolve("dev/mintychochip/ModularJobsBootstrap.java");
    String text = Files.readString(bootstrap);
    assertTrue(text.contains("ProfessionService"));
    assertTrue(text.contains("RecipeService"));
    assertTrue(text.contains("BuffService"));
    assertTrue(text.contains("StationService"));
    assertTrue(text.contains("NodeHarvestService"));
  }

  @Test
  void gsonProviderFactoryCreatesWorkingGson() {
    // Real shipped factory that previously was a Guice Provider
    Gson gson = GsonProvider.create();
    assertNotNull(gson);

    Instant now = Instant.parse("2026-01-15T12:00:00Z");
    String json = gson.toJson(now);
    assertEquals("\"2026-01-15T12:00:00Z\"", json);

    Instant roundTrip = gson.fromJson(json, Instant.class);
    assertEquals(now, roundTrip);
  }

  @Test
  void boostFactoryAvailableWithoutGuice() {
    BoostFactory factory = BoostFactoryImpl.INSTANCE;
    assertNotNull(factory);
  }

  @Test
  void buildFilesDoNotDeclareGuice() throws IOException {
    Path catalog = locateRepoRoot().resolve("gradle/libs.versions.toml");
    Path coreBuild = locateRepoRoot().resolve("paper/build.gradle.kts");
    assertTrue(Files.isRegularFile(catalog));
    assertTrue(Files.isRegularFile(coreBuild));
    assertFalse(Files.readString(catalog).contains("guice"));
    assertFalse(Files.readString(coreBuild).contains("guice"));
    assertTrue(
        Files.readString(coreBuild).contains("libs.guava")
            || Files.readString(coreBuild).contains("guava"),
        "Guava should replace Guice-transitive Guava usage");
  }

  private static Path locateJobsCoreMainJava() {
    Path fromModule = Path.of("src/main/java");
    if (Files.isDirectory(fromModule)) {
      return fromModule.toAbsolutePath().normalize();
    }
    Path fromRoot = Path.of("paper/src/main/java");
    if (Files.isDirectory(fromRoot)) {
      return fromRoot.toAbsolutePath().normalize();
    }
    fail("Could not locate paper/src/main/java from " + Path.of(".").toAbsolutePath());
    return null;
  }

  private static Path locateRepoRoot() {
    Path cwd = Path.of(".").toAbsolutePath().normalize();
    if (Files.isRegularFile(cwd.resolve("settings.gradle.kts"))) {
      return cwd;
    }
    if (Files.isRegularFile(cwd.getParent().resolve("settings.gradle.kts"))) {
      return cwd.getParent();
    }
    fail("Could not locate repo root from " + cwd);
    return null;
  }
}
