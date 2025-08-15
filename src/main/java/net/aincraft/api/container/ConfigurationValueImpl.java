package net.aincraft.api.container;

import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

final class ConfigurationValueImpl<T> implements ConfigurationValue<T> {

  private final Supplier<T> supplier;
  public ConfigurationValueImpl(Supplier<T> supplier) {
    this.supplier = supplier;
  }

  @Override
  public @NotNull T get() throws IllegalStateException {
    T t = supplier.get();
    if (t == null) {
      throw new IllegalStateException("the supplier returned a null expected");
    }
    return t;
  }
}
