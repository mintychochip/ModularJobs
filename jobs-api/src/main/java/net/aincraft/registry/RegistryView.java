package net.aincraft.registry;

import java.util.Optional;
import java.util.stream.Stream;
import javax.swing.text.html.Option;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public interface RegistryView<T> extends Iterable<T> {
  @NotNull
  Optional<T> get(Key key);
  @NotNull
  T getOrThrow(Key key) throws IllegalArgumentException;

  boolean isRegistered(Key key);

  Stream<T> stream();
}
