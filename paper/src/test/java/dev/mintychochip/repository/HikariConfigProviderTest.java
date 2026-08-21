package dev.mintychochip.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zaxxer.hikari.HikariConfig;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

class HikariConfigProviderTest {

  @Test
  void mysqlConfigEnablesPrepStmtCache() {
    MemoryConfiguration section = new MemoryConfiguration();
    section.set("jdbc-url", "jdbc:mysql://127.0.0.1:3306/modularjobs");
    section.set("username", "modularjobs");
    section.set("password", "secret");
    section.set("maximum-pool-size", 10);

    HikariConfig config = new HikariConfigProvider(section, DatabaseType.MYSQL).create();

    assertEquals("true", config.getDataSourceProperties().getProperty("cachePrepStmts"));
    assertEquals("250", config.getDataSourceProperties().getProperty("prepStmtCacheSize"));
  }
}
