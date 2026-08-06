package net.aincraft.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Drives the real {@link DatabaseConfigSections} helper used before ConnectionSourceFactory.
 */
class DatabaseConfigSectionsTest {

  @Test
  void requireSectionReturnsNestedMap() {
    MemoryConfiguration root = new MemoryConfiguration();
    ConfigurationSection payable = root.createSection("payable");
    payable.set("type", "postgres");
    payable.set("jdbc-url", "jdbc:postgresql://localhost:5432/modularjobs");

    ConfigurationSection section = DatabaseConfigSections.requireSection(root, "payable");
    assertEquals("postgres", section.getString("type"));
    assertEquals("jdbc:postgresql://localhost:5432/modularjobs", section.getString("jdbc-url"));
  }

  @Test
  void requireSectionMissingKeyThrowsWithKeyInMessage() {
    MemoryConfiguration root = new MemoryConfiguration();
    root.set("other", "value");

    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> DatabaseConfigSections.requireSection(root, "payable"));
    assertTrue(ex.getMessage().contains("payable"), ex.getMessage());
    assertTrue(
        ex.getMessage().toLowerCase().contains("missing")
            || ex.getMessage().contains("Available keys"),
        ex.getMessage());
  }

  @Test
  void requireSectionNonSectionScalarThrowsWithKeyInMessage() {
    MemoryConfiguration root = new MemoryConfiguration();
    root.set("payable", "not-a-map");

    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> DatabaseConfigSections.requireSection(root, "payable"));
    assertTrue(ex.getMessage().contains("payable"), ex.getMessage());
    assertTrue(
        ex.getMessage().contains("section") || ex.getMessage().contains("map"),
        ex.getMessage());
  }

  @Test
  void requireSectionNullRootThrowsClearly() {
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> DatabaseConfigSections.requireSection(null, "payable"));
    assertTrue(ex.getMessage().contains("payable"), ex.getMessage());
  }

  @Test
  void sectionOrFallbackUsesPreferredWhenPresent() {
    MemoryConfiguration root = new MemoryConfiguration();
    ConfigurationSection payable = root.createSection("payable");
    payable.set("type", "postgres");
    ConfigurationSection upgrades = root.createSection("upgrades");
    upgrades.set("type", "postgres");
    upgrades.set("jdbc-url", "jdbc:postgresql://other:5432/db");

    ConfigurationSection result =
        DatabaseConfigSections.sectionOrFallback(root, "upgrades", payable);
    assertEquals("postgres", result.getString("type"));
    assertEquals("jdbc:postgresql://other:5432/db", result.getString("jdbc-url"));
  }

  @Test
  void sectionOrFallbackReturnsFallbackWhenPreferredAbsent() {
    MemoryConfiguration root = new MemoryConfiguration();
    ConfigurationSection payable = root.createSection("payable");
    payable.set("type", "postgres");

    ConfigurationSection result =
        DatabaseConfigSections.sectionOrFallback(root, "upgrades", payable);
    assertSame(payable, result);
  }

  @Test
  void sectionOrFallbackNonSectionPreferredThrows() {
    MemoryConfiguration root = new MemoryConfiguration();
    ConfigurationSection payable = root.createSection("payable");
    root.set("upgrades", "scalar");

    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> DatabaseConfigSections.sectionOrFallback(root, "upgrades", payable));
    assertTrue(ex.getMessage().contains("upgrades"), ex.getMessage());
  }
}
