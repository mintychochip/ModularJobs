package net.aincraft.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Schema ownership: SQLite may bootstrap in-process; remote never does.
 */
class SchemaPolicyTest {

  @Test
  void sqliteAppliesSchemaByDefault() {
    assertTrue(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.SQLITE, null));
    assertTrue(
        SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.SQLITE, new MemoryConfiguration()));
  }

  @Test
  void sqliteCanDisableBootstrap() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("auto-schema", false);
    assertFalse(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.SQLITE, section));
  }

  @Test
  void remoteNeverAppliesSchemaEvenIfAutoSchemaTrue() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("auto-schema", true);
    assertFalse(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.POSTGRES, section));
    assertFalse(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.MYSQL, section));
    assertFalse(SchemaPolicy.shouldApplySchemaOnConnect(DatabaseType.MARIADB, section));
  }

  @Test
  void remoteVerifiesPresence() {
    assertTrue(SchemaPolicy.shouldVerifySchemaPresent(DatabaseType.POSTGRES));
    assertTrue(SchemaPolicy.shouldVerifySchemaPresent(DatabaseType.MYSQL));
    assertFalse(SchemaPolicy.shouldVerifySchemaPresent(DatabaseType.SQLITE));
  }

  @Test
  void remoteAutoSchemaFlagIsMisconfiguration() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("auto-schema", true);
    assertTrue(SchemaPolicy.hasIgnoredRemoteAutoSchema(DatabaseType.POSTGRES, section));
    assertFalse(SchemaPolicy.hasIgnoredRemoteAutoSchema(DatabaseType.SQLITE, section));
    assertFalse(SchemaPolicy.hasIgnoredRemoteAutoSchema(DatabaseType.POSTGRES, null));
  }
}
