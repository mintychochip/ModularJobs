package dev.mintychochip.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Adapter decoupling a temporary SQL-backed simple key/value repository from the database dialect
 * and schema, defining the query strings the repository executes and the null/parameter/result
 * conversions needed for each operation.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface RelationalRepositoryContext<K, V> {

  /** Returns the select query. */
  String getSelectQuery();

  /** Returns the save query. */
  String getSaveQuery();

  /** Returns the delete query. */
  String getDeleteQuery();

  /** Sets the key. */
  void setKey(PreparedStatement ps, K key) throws SQLException;

  /** Sets the save values. */
  void setSaveValues(PreparedStatement ps, K key, V value) throws SQLException;

  /** Map result. */
  V mapResult(ResultSet rs, K key) throws SQLException;
}
