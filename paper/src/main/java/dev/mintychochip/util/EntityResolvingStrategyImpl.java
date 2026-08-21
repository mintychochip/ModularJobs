package dev.mintychochip.util;

import dev.mintychochip.container.Context.EntityContext;
import dev.mintychochip.util.KeyResolver.KeyResolvingStrategy;
import net.kyori.adventure.key.Key;

/**
 * Resolves entity payment context keys from pure entity-type keys. External live-entity resolution
 * is disabled because contexts carry stable keys, not instances.
 */
public class EntityResolvingStrategyImpl implements KeyResolvingStrategy<EntityContext> {

  @Override
  public Key resolve(EntityContext object) {
    return Key.key(object.entityTypeKey());
  }
}
