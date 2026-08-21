package dev.mintychochip;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards pure modules ({@code api}, {@code common}) against accidental Bukkit/Paper imports.
 */
class ArchitectureIsolationTest {

  @Test
  void apiAndCommonSourcesMustNotImportBukkitOrPaper() throws IOException {
    List<Path> roots = List.of(
        Path.of("src/main/java"),
        // common is sibling — also scan from repo via relative path when test runs from api project
        Path.of("../common/src/main/java"));
    List<String> offenders = new ArrayList<>();
    for (Path root : roots) {
      if (!Files.isDirectory(root)) {
        continue;
      }
      try (Stream<Path> walk = Files.walk(root)) {
        walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
          try {
            String text = Files.readString(p);
            if (text.contains("import org.bukkit")
                || text.contains("import io.papermc")
                || text.contains("import org.spigotmc")
                || text.contains("import de.flog99.mapgui")) {
              offenders.add(p.toString());
            }
          } catch (IOException e) {
            fail(e);
          }
        });
      }
    }
    assertTrue(offenders.isEmpty(), "Bukkit/Paper imports in pure modules: " + offenders);
  }
}
