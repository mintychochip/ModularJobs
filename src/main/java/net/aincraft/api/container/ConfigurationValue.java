package net.aincraft.api.container;

import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

@NonExtendable
public interface ConfigurationValue<T> {

  static <T> ConfigurationValue<T> create(Supplier<T> supplier) {
    return new ConfigurationValueImpl<>(supplier);
  }
  @NotNull
  T get() throws IllegalStateException;
}
