package net.aincraft.api.action;

import java.util.Locale;
import net.aincraft.api.context.Context.BlockContext;
import net.aincraft.api.context.Context.DyeContext;
import net.aincraft.api.context.Context.EnchantmentContext;
import net.aincraft.api.context.Context.EntityContext;
import net.aincraft.api.context.Context.ItemContext;
import net.aincraft.api.context.Context.MaterialContext;
import net.aincraft.api.context.Context.PotionContext;
import net.aincraft.api.context.KeyResolver;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

record ActionTypeImpl(Key key) implements ActionType {

  static {
    KeyResolver.keyResolver()
        .addStrategy(BlockContext.class, context -> context.block().getType().getKey());
    KeyResolver.keyResolver()
        .addStrategy(MaterialContext.class, context -> context.material().getKey());
    KeyResolver.keyResolver().addStrategy(DyeContext.class,
        context -> NamespacedKey.minecraft(context.color().name().toLowerCase(Locale.ENGLISH)));
    KeyResolver.keyResolver()
        .addStrategy(EntityContext.class, context -> context.entity().getType().getKey());
    KeyResolver.keyResolver()
        .addStrategy(ItemContext.class, context -> context.itemStack().getType().getKey());
    KeyResolver.keyResolver().addStrategy(PotionContext.class,
        context -> context.type().key());
    KeyResolver.keyResolver().addStrategy(EnchantmentContext.class, context -> {
      Enchantment enchantment = context.enchantment();
      Key enchantmentKey = enchantment.key();
      return new NamespacedKey(enchantmentKey.namespace(),
          enchantmentKey.value() + "_" + context.level());
    });
  }

  static ActionType create(Key key) {
    ActionTypeImpl type = new ActionTypeImpl(key);
    RegistryContainer.registryContainer()
        .editRegistry(RegistryKeys.ACTION_TYPES, r -> r.register(type));
    return type;
  }

  @Override
  public @NotNull Key key() {
    return key;
  }
}
