package dev.mintychochip.container;

import org.jetbrains.annotations.ApiStatus.NonExtendable;

/**
 * Pure payment/action context (no Bukkit types). Paper maps Bukkit objects via {@code
 * dev.mintychochip.paper.BukkitContexts}.
 */
@NonExtendable
public sealed interface Context
    permits Context.BlockContext,
        Context.ChunkContext,
        Context.DyeContext,
        Context.EnchantmentContext,
        Context.EntityContext,
        Context.ItemContext,
        Context.MaterialContext,
        Context.PotionContext {

  /** Block at location; materialKey like "minecraft:stone". */
  record BlockContext(String worldName, int x, int y, int z, String materialKey)
      implements Context {}

  /** Item material key (+ amount). */
  record ItemContext(String materialKey, int amount) implements Context {}

  /** Type. */
  @Deprecated
  record MaterialContext(String materialKey) implements Context {}

  /** Entity type key like "minecraft:zombie". */
  record EntityContext(String entityTypeKey) implements Context {}

  /** Dye context. */
  record DyeContext(String dyeColorName) implements Context {}

  /** Enchantment context. */
  record EnchantmentContext(String enchantmentKey, int level) implements Context {}

  /** Potion context. */
  record PotionContext(String potionTypeKey) implements Context {}

  /** Chunk context. */
  record ChunkContext(String worldName, int chunkX, int chunkZ) implements Context {}
}
