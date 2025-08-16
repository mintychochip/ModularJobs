package net.aincraft.registry;

import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;

public interface Registry<T extends Keyed> extends RegistryView<T> {

  static <T extends Keyed> Registry<T> simple() {
    return new SimpleRegistryImpl<>();
  }

  void register(@NotNull T object);

}
