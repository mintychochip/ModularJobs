package net.aincraft.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Proves pool identity keying for shared Postgres jdbc-url + username.
 */
class SharedConnectionSourcesTest {

  @Test
  void poolIdentityMatchesSameJdbcAndUser() {
    YamlConfiguration a = new YamlConfiguration();
    a.set("type", "postgres");
    a.set("jdbc-url", "jdbc:postgresql://localhost:5432/modularjobs");
    a.set("username", "modularjobs");
    a.set("password", "secret");

    YamlConfiguration b = new YamlConfiguration();
    b.set("type", "postgres");
    b.set("jdbc-url", "jdbc:postgresql://localhost:5432/modularjobs");
    b.set("username", "modularjobs");
    b.set("password", "other");

    assertEquals(
        SharedConnectionSources.poolIdentity(a),
        SharedConnectionSources.poolIdentity(b),
        "same jdbc-url+username must share pool identity");
  }

  @Test
  void poolIdentityDiffersForDifferentUsers() {
    YamlConfiguration a = new YamlConfiguration();
    a.set("jdbc-url", "jdbc:postgresql://localhost:5432/modularjobs");
    a.set("username", "alice");

    YamlConfiguration b = new YamlConfiguration();
    b.set("jdbc-url", "jdbc:postgresql://localhost:5432/modularjobs");
    b.set("username", "bob");

    assertNotEquals(
        SharedConnectionSources.poolIdentity(a),
        SharedConnectionSources.poolIdentity(b));
  }
}
