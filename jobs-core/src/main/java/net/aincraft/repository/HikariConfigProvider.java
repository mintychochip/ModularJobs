package net.aincraft.repository;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

final class HikariConfigProvider {

  @NotNull
  private final ConfigurationSection configuration;

  HikariConfigProvider(@NotNull ConfigurationSection configuration) {
    this.configuration = configuration;
  }

  @NotNull
  public HikariConfig create()
      throws IllegalStateException {
    HikariConfig hikariConfig = new HikariConfig();

    String jdbcUrl = configuration.getString("jdbc-url");
    String username = configuration.getString("username");
    String password = configuration.getString("password");

    Preconditions.checkNotNull(jdbcUrl, "missing required field: database.jdbc-url");
    Preconditions.checkNotNull(username, "missing required field: database.username");
    Preconditions.checkNotNull(password, "missing required field: database.password");

    hikariConfig.setJdbcUrl(jdbcUrl);
    hikariConfig.setUsername(username);
    hikariConfig.setPassword(password);

    int maxPoolSize = configuration.getInt("maximum-pool-size", -1);
    if (maxPoolSize > 0) {
      hikariConfig.setMaximumPoolSize(maxPoolSize);
    }

    int minIdle = configuration.getInt("minimum-idle", -1);
    if (minIdle >= 0) {
      hikariConfig.setMinimumIdle(minIdle);
    }

    long connectionTimeout = configuration.getLong("connection-timeout", -1);
    if (connectionTimeout > 0) {
      hikariConfig.setConnectionTimeout(connectionTimeout);
    }

    long idleTimeout = configuration.getLong("idle-timeout", -1);
    if (idleTimeout > 0) {
      hikariConfig.setIdleTimeout(idleTimeout);
    }

    long maxLifetime = configuration.getLong("max-lifetime", -1);
    if (maxLifetime > 0) {
      hikariConfig.setMaxLifetime(maxLifetime);
    }
    return hikariConfig;
  }
}
