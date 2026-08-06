package net.aincraft.container;

import net.aincraft.container.Context.BlockContext;
import net.aincraft.container.Context.ChunkContext;
import net.aincraft.container.Context.DyeContext;
import net.aincraft.container.Context.EnchantmentContext;
import net.aincraft.container.Context.EntityContext;
import net.aincraft.container.Context.ItemContext;
import net.aincraft.container.Context.MaterialContext;
import net.aincraft.container.Context.PotionContext;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public sealed interface Context permits BlockContext, ChunkContext, DyeContext, EnchantmentContext, EntityContext,
    ItemContext, MaterialContext, PotionContext {

  record BlockContext(Block block) implements Context {

  }

  record ItemContext(ItemStack itemStack) implements Context {

  }

  @Deprecated
  record MaterialContext(Material material) implements Context {

  }

  record EntityContext(Entity entity) implements Context {

  }

  record DyeContext(DyeColor color) implements Context {

  }

  record EnchantmentContext(Enchantment enchantment, int level) implements Context {

  }

  record PotionContext(PotionType type) implements Context {

  }

  record ChunkContext(Chunk chunk) implements Context {

  }
}
