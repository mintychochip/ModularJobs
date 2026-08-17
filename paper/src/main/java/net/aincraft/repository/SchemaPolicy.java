package net.aincraft.repository;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Schema ownership for ModularJobs (MySQL only).
 *
 * <p>The plugin process never runs DDL. Ops provision tables from
 * {@code sql/mysql.sql} via {@code scripts/apply-mysql-schema.sh} (or equivalent).
 * On connect the plugin only verifies required tables exist.
 *
 * <p>Config key {@code auto-schema} is ignored (logged as a misconfiguration if present).
 */
public final class SchemaPolicy {

  private SchemaPolicy() {}

  /**
   * @return always false — never CREATE TABLE inside the game process
   */
  public static boolean shouldApplySchemaOnConnect(
      @NotNull DatabaseType type, @Nullable ConfigurationSection configuration) {
    return false;
  }

  /**
   * @return true — fail fast when ops has not applied {@code sql/mysql.sql}
   */
  public static boolean shouldVerifySchemaPresent(@NotNull DatabaseType type) {
    return type == DatabaseType.MYSQL;
  }

  /**
   * @return true if config still sets {@code auto-schema} (unsupported; ops must apply DDL)
   */
  public static boolean hasIgnoredRemoteAutoSchema(
      @NotNull DatabaseType type, @Nullable ConfigurationSection configuration) {
    return configuration != null && configuration.contains("auto-schema");
  }
}
