package net.aincraft.repository;

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

  /** SELECT statement resolving a single row by key. */
  String getSelectQuery();

  /** INSERT/UPDATE statement persisting a value by key. */
  String getSaveQuery();

  /** DELETE statement removing a row by key. */
  String getDeleteQuery();

  /** Binds the key onto the prepared statement for select/delete operations. */
  void setKey(PreparedStatement ps, K key) throws SQLException;

  /** Binds the key and value onto the prepared statement for a save operation. */
  void setSaveValues(PreparedStatement ps, K key, V value) throws SQLException;

  /** Maps a single result row to a value for the given key. */
  V mapResult(ResultSet rs, K key) throws SQLException;
}
