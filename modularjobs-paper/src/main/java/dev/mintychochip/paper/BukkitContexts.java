package dev.mintychochip.paper;

import dev.mintychochip.container.Context;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

/** Maps Bukkit types to pure {@link Context} variants for the payment path. */
public final class BukkitContexts {

  private BukkitContexts() {}

  /** Block. */
  public static Context.BlockContext block(@NotNull Block block) {
    Objects.requireNonNull(block, "block");
    return new Context.BlockContext(
        block.getWorld().getName(),
        block.getX(),
        block.getY(),
        block.getZ(),
        block.getType().getKey().toString());
  }

  /** Item. */
  public static Context.ItemContext item(@NotNull ItemStack stack) {
    Objects.requireNonNull(stack, "stack");
    return new Context.ItemContext(stack.getType().getKey().toString(), stack.getAmount());
  }

  /** API member. */
  @Deprecated
  public static Context.MaterialContext material(@NotNull Material material) {
    Objects.requireNonNull(material, "material");
    return new Context.MaterialContext(material.getKey().toString());
  }

  /** Entity. */
  public static Context.EntityContext entity(@NotNull Entity entity) {
    Objects.requireNonNull(entity, "entity");
    return new Context.EntityContext(entity.getType().getKey().toString());
  }

  /** Dye. */
  public static Context.DyeContext dye(@NotNull DyeColor color) {
    Objects.requireNonNull(color, "color");
    return new Context.DyeContext(color.name().toLowerCase(Locale.ENGLISH));
  }

  /** Enchantment. */
  public static Context.EnchantmentContext enchantment(
      @NotNull Enchantment enchantment, int level) {
    Objects.requireNonNull(enchantment, "enchantment");
    return new Context.EnchantmentContext(enchantment.getKey().toString(), level);
  }

  /** Potion. */
  public static Context.PotionContext potion(@NotNull PotionType type) {
    Objects.requireNonNull(type, "type");
    return new Context.PotionContext(type.getKey().toString());
  }

  /** Chunk. */
  public static Context.ChunkContext chunk(@NotNull Chunk chunk) {
    Objects.requireNonNull(chunk, "chunk");
    return new Context.ChunkContext(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
  }
}
