package net.aincraft.repository;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Schema ownership rules for ModularJobs.
 *
 * <ul>
 *   <li><b>SQLite (local file):</b> the plugin may apply {@code sql/sqlite.sql} on connect.
 *       Zero-config Paper installs need that exception.
 *   <li><b>Postgres / MySQL / MariaDB (remote or shared):</b> the runtime process never runs
 *       DDL. Schema is provisioned out-of-band from {@code sql/&lt;dialect&gt;.sql}
 *       (see {@code scripts/apply-postgres-schema.sh}).
 * </ul>
 *
 * <p>Config key {@code auto-schema} is ignored for non-SQLite dialects (and logged as a
 * misconfiguration if present). For SQLite it can force bootstrap off with
 * {@code auto-schema: false}.
 */
public final class SchemaPolicy {

  private SchemaPolicy() {}

  /**
   * @return true only when this JVM process should execute shipped DDL on connect
   */
  public static boolean shouldApplySchemaOnConnect(
      @NotNull DatabaseType type, @Nullable ConfigurationSection configuration) {
    if (type != DatabaseType.SQLITE) {
      // Remote / shared: never create tables inside the game process.
      return false;
    }
    if (configuration != null && configuration.contains("auto-schema")) {
      return configuration.getBoolean("auto-schema");
    }
    return true;
  }

  /**
   * @return true when the process should verify required tables already exist (fail fast)
   */
  public static boolean shouldVerifySchemaPresent(
      @NotNull DatabaseType type) {
    return type == DatabaseType.POSTGRES
        || type == DatabaseType.MYSQL
        || type == DatabaseType.MARIADB;
  }

  /**
   * @return true if config still sets {@code auto-schema} on a remote dialect (misconfig)
   */
  public static boolean hasIgnoredRemoteAutoSchema(
      @NotNull DatabaseType type, @Nullable ConfigurationSection configuration) {
    return type != DatabaseType.SQLITE
        && configuration != null
        && configuration.contains("auto-schema");
  }
}
