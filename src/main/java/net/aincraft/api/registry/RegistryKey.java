package net.aincraft.api.registry;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

public sealed interface RegistryKey<T> extends Keyed permits RegistryKeyImpl {

  static <T> RegistryKey<T> key(Key key) {
    return new RegistryKeyImpl<>(key);
  }

}
