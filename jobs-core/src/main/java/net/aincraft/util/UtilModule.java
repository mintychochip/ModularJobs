package net.aincraft.util;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.Locale;
import net.aincraft.container.Context.BlockContext;
import net.aincraft.container.Context.DyeContext;
import net.aincraft.container.Context.EnchantmentContext;
import net.aincraft.container.Context.EntityContext;
import net.aincraft.container.Context.ItemContext;
import net.aincraft.container.Context.MaterialContext;
import net.aincraft.container.Context.PotionContext;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

public final class UtilModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(KeyFactory.class).to(KeyFactoryImpl.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  public KeyResolver keyResolver() {
    KeyResolverImpl resolver = new KeyResolverImpl();
    resolver.addStrategy(BlockContext.class, context -> context.block().getType().getKey());
    resolver.addStrategy(MaterialContext.class, context -> context.material().getKey());
    resolver.addStrategy(DyeContext.class,
        context -> NamespacedKey.minecraft(context.color().name().toLowerCase(Locale.ENGLISH)));
    resolver.addStrategy(EntityContext.class, context -> context.entity().getType().getKey());
    resolver.addStrategy(ItemContext.class, context -> context.itemStack().getType().getKey());
    resolver.addStrategy(PotionContext.class,
        context -> context.type().key());
    resolver.addStrategy(EnchantmentContext.class, context -> {
      Enchantment enchantment = context.enchantment();
      Key enchantmentKey = enchantment.key();
      return new NamespacedKey(enchantmentKey.namespace(),
          enchantmentKey.value() + "_" + context.level());
    });
    return resolver;
  }
}
