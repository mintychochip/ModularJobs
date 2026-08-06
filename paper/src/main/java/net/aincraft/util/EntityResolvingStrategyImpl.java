package net.aincraft.util;

import net.aincraft.container.Context.EntityContext;
import net.aincraft.util.KeyResolver.KeyResolvingStrategy;
import net.kyori.adventure.key.Key;

/**
 * Resolves entity payment context keys from pure entity-type keys.
 * MythicMobs live-entity resolution is disabled (entity instance no longer on Context).
 */
public class EntityResolvingStrategyImpl implements KeyResolvingStrategy<EntityContext> {

  @Override
  public Key resolve(EntityContext object) {
    return Key.key(object.entityTypeKey());
  }
}
