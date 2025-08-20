package net.aincraft.util;

import net.kyori.adventure.key.Key;

public interface KeyFactory {
  Key create(String raw) throws IllegalArgumentException;
}
