package net.aincraft.util;

import java.util.Locale;
import net.aincraft.container.Context.BlockContext;
import net.aincraft.container.Context.ChunkContext;
import net.aincraft.container.Context.DyeContext;
import net.aincraft.container.Context.EnchantmentContext;
import net.aincraft.container.Context.EntityContext;
import net.aincraft.container.Context.ItemContext;
import net.aincraft.container.Context.MaterialContext;
import net.aincraft.container.Context.PotionContext;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * Factory for the shared {@link KeyResolver} (replaces Guice UtilModule).
 * Strategies read pure {@link net.aincraft.container.Context} string/coord fields.
 */
public final class KeyResolvers {

  /** Prevents instantiation of this static factory class. */
  private KeyResolvers() {}

  /**
   * Creates a resolver with strategies for all built-in context types.
   *
   * <p>Chunk contexts use a loaded world's key when possible and otherwise
   * normalize the configured world name into a key.
   *
   * @return configured resolver
   */
  public static KeyResolver create() {
    KeyResolver resolver = new KeyResolver();
    resolver.addStrategy(BlockContext.class, context -> Key.key(context.materialKey()));
    resolver.addStrategy(MaterialContext.class, context -> Key.key(context.materialKey()));
    resolver.addStrategy(DyeContext.class,
        context -> Key.key("minecraft", context.dyeColorName().toLowerCase(Locale.ENGLISH)));
    resolver.addStrategy(EntityContext.class, new EntityResolvingStrategyImpl());
    resolver.addStrategy(ItemContext.class, context -> Key.key(context.materialKey()));
    resolver.addStrategy(PotionContext.class, context -> Key.key(context.potionTypeKey()));
    resolver.addStrategy(EnchantmentContext.class, context -> {
      Key base = Key.key(context.enchantmentKey());
      return Key.key(base.namespace(), base.value() + "_" + context.level());
    });
    // Explore tasks historically keyed by world NamespacedKey (not display name).
    resolver.addStrategy(ChunkContext.class, context -> {
      World world = Bukkit.getWorld(context.worldName());
      if (world != null) {
        return world.getKey();
      }
      String name = context.worldName();
      if (name.indexOf(':') >= 0) {
        return Key.key(name);
      }
      return Key.key("minecraft", name.toLowerCase(Locale.ENGLISH));
    });
    return resolver;
  }
}
