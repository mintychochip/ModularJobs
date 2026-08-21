package dev.mintychochip.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads packaged SQL statement text from {@code /sql/} classpath resources.
 */
public final class SqlStatements {

  private SqlStatements() {}

  /**
   * Reads the SQL resource {@code name}. Relative names are resolved under {@code /sql/}.
   *
   * @param name resource name such as {@code job_tasks/select-task-id.sql} or an absolute
   *     classpath path starting with {@code /}
   * @return trimmed statement text
   * @throws IllegalStateException if the resource is missing
   */
  public static String load(String name) {
    String path = name.startsWith("/") ? name : "/sql/" + name;
    try (InputStream in = SqlStatements.class.getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalStateException("missing SQL resource: " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
