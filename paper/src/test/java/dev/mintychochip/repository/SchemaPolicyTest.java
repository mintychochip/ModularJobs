package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

/** Schema ownership: MySQL only; plugin never applies DDL in-process. */
class SchemaPolicyTest {

  @Test
  void neverAppliesSchemaOnConnect() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("auto-schema", true);
    assertFalse(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.MYSQL, null));
    assertFalse(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.MYSQL, section));
  }

  @Test
  void alwaysVerifiesPresence() {
    assertTrue(SchemaPolicy.shouldVerifySchemaPresent(DatabaseType.MYSQL));
  }

  @Test
  void autoSchemaFlagIsMisconfiguration() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("auto-schema", true);
    assertTrue(SchemaPolicy.hasIgnoredRemoteAutoSchema(DatabaseType.MYSQL, section));
    assertFalse(SchemaPolicy.hasIgnoredRemoteAutoSchema(DatabaseType.MYSQL, null));
  }
}
