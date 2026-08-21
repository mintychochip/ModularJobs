package dev.mintychochip.profession;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Structural check: every §8.1 storage key has a jobs.yml entry (shipped resource).
 */
class ProfessionCatalogJobsYmlAlignmentTest {

  private static final Pattern TOP_LEVEL_KEY = Pattern.compile("^([a-z0-9_]+):\\s*$", Pattern.MULTILINE);

  @Test
  void everyCatalogStorageKeyExistsInJobsYml() throws IOException {
    String yml;
    try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("jobs.yml")) {
      assertTrue(in != null, "jobs.yml must be on test classpath from paper resources");
      yml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    Set<String> jobKeys = new HashSet<>();
    Matcher m = TOP_LEVEL_KEY.matcher(yml);
    while (m.find()) {
      jobKeys.add(m.group(1));
    }

    for (ProfessionDefinition track : ProfessionCatalog.tracks()) {
      assertTrue(
          jobKeys.contains(track.storageKey()),
          "jobs.yml missing storage key for " + track.id() + " → " + track.storageKey()
              + "; present keys=" + jobKeys);
    }
  }
}
