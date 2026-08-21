package net.aincraft.repository;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

/**
 * JDBC {@link Connection} wrapper that ignores {@code close()} so try-with-resources
 * in repositories can share one connection across operations (e.g. unit tests).
 * Call {@link #shutdown()} to actually close the delegate.
 */
@Internal
public interface NonClosableConnection extends Connection {

  static NonClosableConnection create(@NotNull Connection delegate) {
    return (NonClosableConnection) Proxy.newProxyInstance(
        Thread.currentThread().getContextClassLoader(),
        new Class[]{NonClosableConnection.class}, (proxy, method, args) -> {
          if ("close".equals(method.getName())) {
            return null;
          }
          if ("shutdown".equals(method.getName())) {
            delegate.close();
            return null;
          }
          return method.invoke(delegate, args);
        });
  }

  void shutdown() throws SQLException;
}
