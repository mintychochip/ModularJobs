package net.aincraft.api.context;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

public interface KeyResolver<C> {
  @NotNull
  Key resolve(C object) throws IllegalArgumentException;
}
