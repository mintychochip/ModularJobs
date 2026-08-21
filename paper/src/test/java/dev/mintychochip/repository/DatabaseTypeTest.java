package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DatabaseTypeTest {

  @Test
  void defaultIsMysql() {
    assertEquals(DatabaseType.MYSQL, DatabaseType.getDefault());
  }

  @Test
  void acceptsMysqlCaseInsensitively() {
    assertEquals(DatabaseType.MYSQL, DatabaseType.fromIdentifier("mysql"));
    assertEquals(DatabaseType.MYSQL, DatabaseType.fromIdentifier("MYSQL"));
  }

  @Test
  void blankIdentifierDefaultsToMysql() {
    assertEquals(DatabaseType.MYSQL, DatabaseType.fromIdentifier(null));
    assertEquals(DatabaseType.MYSQL, DatabaseType.fromIdentifier(""));
    assertEquals(DatabaseType.MYSQL, DatabaseType.fromIdentifier("  "));
  }

  @Test
  void rejectsPostgresAndUnknownIdentifiers() {
    for (String id : new String[] {"postgres", "postgresql", "sqlite", "mariadb", "mongo", "h2"}) {
      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> DatabaseType.fromIdentifier(id));
      assertTrue(ex.getMessage().toLowerCase().contains("mysql"),
          "message should mention mysql-only for " + id + ": " + ex.getMessage());
    }
  }

  @Test
  void exposesMysqlDriverAndOnlyEnumConstant() {
    assertEquals("mysql", DatabaseType.MYSQL.getIdentifier());
    assertEquals("com.mysql.cj.jdbc.Driver", DatabaseType.MYSQL.getClassName());
    assertEquals(1, DatabaseType.values().length);
    assertEquals(DatabaseType.MYSQL, DatabaseType.values()[0]);
  }
}
