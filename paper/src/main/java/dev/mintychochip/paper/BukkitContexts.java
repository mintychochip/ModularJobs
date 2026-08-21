package dev.mintychochip.paper;

import java.util.Locale;
import java.util.Objects;
import dev.mintychochip.container.Context;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

/**
 * Maps Bukkit types to pure {@link Context} variants for the payment path.
 */
public final class BukkitContexts {

  private BukkitContexts() {}

  public static Context.BlockContext block(@NotNull Block block) {
    Objects.requireNonNull(block, "block");
    return new Context.BlockContext(
        block.getWorld().getName(),
        block.getX(),
        block.getY(),
        block.getZ(),
        block.getType().getKey().toString());
  }

  public static Context.ItemContext item(@NotNull ItemStack stack) {
    Objects.requireNonNull(stack, "stack");
    return new Context.ItemContext(stack.getType().getKey().toString(), stack.getAmount());
  }

  @Deprecated
  public static Context.MaterialContext material(@NotNull Material material) {
    Objects.requireNonNull(material, "material");
    return new Context.MaterialContext(material.getKey().toString());
  }

  public static Context.EntityContext entity(@NotNull Entity entity) {
    Objects.requireNonNull(entity, "entity");
    return new Context.EntityContext(entity.getType().getKey().toString());
  }

  public static Context.DyeContext dye(@NotNull DyeColor color) {
    Objects.requireNonNull(color, "color");
    return new Context.DyeContext(color.name().toLowerCase(Locale.ENGLISH));
  }

  public static Context.EnchantmentContext enchantment(@NotNull Enchantment enchantment, int level) {
    Objects.requireNonNull(enchantment, "enchantment");
    return new Context.EnchantmentContext(enchantment.getKey().toString(), level);
  }

  public static Context.PotionContext potion(@NotNull PotionType type) {
    Objects.requireNonNull(type, "type");
    return new Context.PotionContext(type.getKey().toString());
  }

  public static Context.ChunkContext chunk(@NotNull Chunk chunk) {
    Objects.requireNonNull(chunk, "chunk");
    return new Context.ChunkContext(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
  }
}
