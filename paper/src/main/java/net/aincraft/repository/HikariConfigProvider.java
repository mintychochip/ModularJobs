package net.aincraft.repository;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * Builds a {@link HikariConfig} from the plugin's database configuration section.
 *
 * <p>Requires the {@code jdbc-url}, {@code username}, and {@code password} fields and applies
 * optional pool sizing and timeout settings ({@code maximum-pool-size}, {@code minimum-idle},
 * {@code connection-timeout}, {@code idle-timeout}, {@code max-lifetime}) when present.
 */
final class HikariConfigProvider {

  @NotNull
  private final ConfigurationSection configuration;

  @NotNull
  private final DatabaseType databaseType;

  HikariConfigProvider(@NotNull ConfigurationSection configuration, @NotNull DatabaseType databaseType) {
    this.configuration = configuration;
    this.databaseType = databaseType;
  }

  /**
   * Creates a Hikari configuration from the configured database settings.
   *
   * @return populated {@link HikariConfig}
   */
  @NotNull
  HikariConfig create() {
    final HikariConfig hikariConfig = new HikariConfig();

    String jdbcUrl = configuration.getString("jdbc-url");
    String username = configuration.getString("username");
    String password = configuration.getString("password");

    Preconditions.checkNotNull(jdbcUrl, "missing required field: database.jdbc-url");
    Preconditions.checkNotNull(username, "missing required field: database.username");
    Preconditions.checkNotNull(password, "missing required field: database.password");

    hikariConfig.setJdbcUrl(jdbcUrl);
    hikariConfig.setUsername(username);
    hikariConfig.setPassword(password);

    String driverClass = databaseType.getClassName();
    if (driverClass != null && !driverClass.isBlank()) {
      hikariConfig.setDriverClassName(driverClass);
    }

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
