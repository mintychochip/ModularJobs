package net.aincraft.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * Supported store for ModularJobs. PostgreSQL only — schema is applied out-of-band
 * ({@code sql/postgres.sql} / {@code scripts/apply-postgres-schema.sh}).
 */
public enum DatabaseType {
  POSTGRES("postgres", "org.postgresql.Driver");

  @NotNull
  private final String identifier;
  private final String className;

  DatabaseType(@NotNull String identifier, String className) {
    this.identifier = identifier;
    this.className = className;
  }

  public @NotNull String getIdentifier() {
    return identifier;
  }

  public String getClassName() {
    return className;
  }

  public String[] getSQLTables() {
    try (InputStream resourceStream = ResourceExtractor.getResourceStream(
        String.format("sql/%s.sql", identifier));
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
      Stream<String> lines = reader.lines();
      String tables = lines.collect(Collectors.joining("\n"));
      return Arrays.stream(tables.split(";"))
          .map(s -> s.trim() + ";")
          .filter(s -> !s.equals(";"))
          .toArray(String[]::new);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @throws IllegalArgumentException if identifier is not {@code postgres}
   */
  public static DatabaseType fromIdentifier(String identifier) {
    if (identifier == null || identifier.isBlank()) {
      return getDefault();
    }
    String id = identifier.trim().toLowerCase();
    if (POSTGRES.identifier.equals(id) || "postgresql".equals(id)) {
      return POSTGRES;
    }
    throw new IllegalArgumentException(
        "Unsupported database type '" + identifier
            + "'. ModularJobs supports PostgreSQL only (type: postgres). "
            + "Provision schema with scripts/apply-postgres-schema.sh");
  }

  public static DatabaseType getDefault() {
    return POSTGRES;
  }
}
