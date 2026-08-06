package net.aincraft.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DatabaseTypeTest {

  @Test
  void defaultIsPostgres() {
    assertEquals(DatabaseType.POSTGRES, DatabaseType.getDefault());
  }

  @Test
  void acceptsPostgresAliases() {
    assertEquals(DatabaseType.POSTGRES, DatabaseType.fromIdentifier("postgres"));
    assertEquals(DatabaseType.POSTGRES, DatabaseType.fromIdentifier("postgresql"));
    assertEquals(DatabaseType.POSTGRES, DatabaseType.fromIdentifier("POSTGRES"));
  }

  @Test
  void rejectsSqliteMysqlMariaAndUnknown() {
    for (String id : new String[] {"sqlite", "mysql", "mariadb", "mongo", "h2"}) {
      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> DatabaseType.fromIdentifier(id));
      assertTrue(ex.getMessage().toLowerCase().contains("postgres"),
          "message should mention postgres-only for " + id + ": " + ex.getMessage());
    }
  }

  @Test
  void onlyOneEnumConstant() {
    assertEquals(1, DatabaseType.values().length);
    assertEquals(DatabaseType.POSTGRES, DatabaseType.values()[0]);
  }
}
