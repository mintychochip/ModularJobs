package dev.mintychochip.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Supported store for ModularJobs. MySQL only — schema is applied out-of-band ({@code
 * sql/mysql.sql} / {@code scripts/apply-mysql-schema.sh}).
 */
public enum DatabaseType {
  MYSQL("mysql", "com.mysql.cj.jdbc.Driver");

  @NotNull private final String identifier;
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

  /**
   * Returns the shipped DDL statements for this database type.
   *
   * <p>The file is split on semicolons that are not inside SQL comments or string literals, so
   * comment lines such as {@code -- Apply out-of-band; the plugin never runs DDL.} do not create
   * partial statements.
   */
  public String[] getSqlTables() {
    try (InputStream resourceStream =
            ResourceExtractor.getResourceStream(String.format("sql/%s.sql", identifier));
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
      String sql = reader.lines().collect(Collectors.joining("\n"));
      return splitStatements(sql);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Splits SQL on statement terminators ({@code ;}) while respecting single-line comments ({@code
   * --}), block comments ({@code /* * /}), and single/double-quoted string literals.
   */
  private static String[] splitStatements(String sql) {
    List<String> statements = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inSingleQuote = false;
    boolean inDoubleQuote = false;
    boolean inLineComment = false;
    boolean inBlockComment = false;
    boolean escapeNext = false;

    int i = 0;
    while (i < sql.length()) {
      char c = sql.charAt(i);

      if (inLineComment) {
        if (c == '\n') {
          inLineComment = false;
        }
        i++;
        continue;
      }

      if (inBlockComment) {
        if (c == '*' && i + 1 < sql.length() && sql.charAt(i + 1) == '/') {
          inBlockComment = false;
          i += 2;
        } else {
          i++;
        }
        continue;
      }

      if (inSingleQuote || inDoubleQuote) {
        current.append(c);
        if (escapeNext) {
          escapeNext = false;
        } else if (c == '\\') {
          escapeNext = true;
        } else if (c == '\'' && inSingleQuote) {
          inSingleQuote = false;
        } else if (c == '"' && inDoubleQuote) {
          inDoubleQuote = false;
        }
        i++;
        continue;
      }

      if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
        inLineComment = true;
        i += 2;
        continue;
      }

      if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
        inBlockComment = true;
        i += 2;
        continue;
      }

      if (c == '\'') {
        inSingleQuote = true;
        current.append(c);
        i++;
        continue;
      }

      if (c == '"') {
        inDoubleQuote = true;
        current.append(c);
        i++;
        continue;
      }

      if (c == ';') {
        String stmt = current.toString().trim();
        if (!stmt.isEmpty()) {
          statements.add(stmt + ";");
        }
        current.setLength(0);
      } else {
        current.append(c);
      }
      i++;
    }

    String remaining = current.toString().trim();
    if (!remaining.isEmpty()) {
      statements.add(remaining + ";");
    }

    return statements.toArray(new String[0]);
  }

  /**
   * Resolves a configured database identifier, defaulting blank values to MySQL.
   *
   * @throws IllegalArgumentException if identifier is not {@code mysql}
   */
  public static DatabaseType fromIdentifier(String identifier) {
    if (identifier == null || identifier.isBlank()) {
      return getDefault();
    }
    String id = identifier.trim().toLowerCase();
    if (MYSQL.identifier.equals(id)) {
      return MYSQL;
    }
    throw new IllegalArgumentException(
        "Unsupported database type '"
            + identifier
            + "'. ModularJobs supports MySQL only (type: mysql). "
            + "Provision schema with scripts/apply-mysql-schema.sh");
  }

  public static DatabaseType getDefault() {
    return MYSQL;
  }
}
