package net.aincraft.api;

import net.aincraft.api.ActionType.Resolving;
import net.aincraft.api.context.KeyResolver;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ActionTypes {

  @Internal
  private static final KeyResolver<Material> MATERIAL_RESOLVER = Material::getKey;

  public static final ActionType.Resolving<Material> BLOCK_PLACE = resolving(MATERIAL_RESOLVER,
      Key.key("jobs:block_place"));

  public static final ActionType.Resolving<Material> BLOCK_BREAK = resolving(MATERIAL_RESOLVER,
      Key.key("jobs:block_break"));

  private static <C> ActionType.Resolving<C> resolving(KeyResolver<C> resolver, Key key) {
    return new ResolvingImpl<>(resolver, key);
  }

  private record ResolvingImpl<C>(KeyResolver<C> resolver, Key key) implements
      Resolving<C> {

  }

}
