package dev.mintychochip.registry;

import net.kyori.adventure.key.Key;

/** Token record wrapping the Adventure key that identifies a registry. */
record RegistryKeyImpl<T>(Key key) implements RegistryKey<T> {}
