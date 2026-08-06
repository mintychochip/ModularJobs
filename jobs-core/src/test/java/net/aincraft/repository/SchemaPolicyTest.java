package net.aincraft.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Schema ownership: PostgreSQL only; plugin never applies DDL in-process.
 */
class SchemaPolicyTest {

  @Test
  void neverAppliesSchemaOnConnect() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("auto-schema", true);
    assertFalse(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.POSTGRES, null));
    assertFalse(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.POSTGRES, section));
  }

  @Test
  void alwaysVerifiesPresence() {
    assertTrue(SchemaPolicy.shouldVerifySchemaPresent(DatabaseType.POSTGRES));
  }

  @Test
  void autoSchemaFlagIsMisconfiguration() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("auto-schema", true);
    assertTrue(SchemaPolicy.hasIgnoredRemoteAutoSchema(DatabaseType.POSTGRES, section));
    assertFalse(SchemaPolicy.hasIgnoredRemoteAutoSchema(DatabaseType.POSTGRES, null));
  }
}
