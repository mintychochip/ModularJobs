package net.aincraft.api.context;

import net.aincraft.api.context.Context.BlockContext;
import net.aincraft.api.context.Context.DyeContext;
import net.aincraft.api.context.Context.EnchantmentContext;
import net.aincraft.api.context.Context.EntityContext;
import net.aincraft.api.context.Context.ItemContext;
import net.aincraft.api.context.Context.MaterialContext;
import net.aincraft.api.context.Context.PotionContext;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public sealed interface Context permits BlockContext, DyeContext, EnchantmentContext, EntityContext,
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
}
