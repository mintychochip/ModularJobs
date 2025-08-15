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
import net.aincraft.api.registry.RegistryView;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

public final class ActionTypes {

  private ActionTypes() {
  }

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

  /**
   * Represents a player placing a block.
   */
  public static final ActionType BLOCK_PLACE = type("block_place");

  /**
   * Represents a player breaking a block.
   */
  public static final ActionType BLOCK_BREAK = type("block_break");

  public static final ActionType TNT_BREAK = type("tnt_break");

  /**
   * Represents a player killing an entity.
   */
  public static final ActionType KILL = type("kill");

  /**
   * Represents a player dyeing a sheep or dyeable entity.
   */
  public static final ActionType DYE = type("dye");

  /**
   * Represents a player stripping logs with an axe.
   */
  public static final ActionType STRIP_LOG = type("strip_log");

  /**
   * Represents a player crafting an item.
   */
  public static final ActionType CRAFT = type("craft");

  /**
   * Represents a player catching a fish.
   */
  public static final ActionType FISH = type("fish");

  /**
   * Represents a player smelting an item in a furnace.
   */
  public static final ActionType SMELT = type("smelt");

  /**
   * Represents a player brewing a potion.
   */
  public static final ActionType BREW = type("brew");

  /**
   * Represents a player enchanting an item.
   */
  public static final ActionType ENCHANT = type("enchant");

  /**
   * Represents a player repairing an item.
   */
  public static final ActionType REPAIR = type("repair");

  /**
   * Represents a player breeding two animals.
   */
  public static final ActionType BREED = type("breed");

  /**
   * Represents a player taming an animal.
   */
  public static final ActionType TAME = type("tame");

  /**
   * Represents a player shearing an entity such as a sheep.
   */
  public static final ActionType SHEAR = type("shear");

  /**
   * Represents a player milking an entity.
   */
  public static final ActionType MILK = type("milk");

  /**
   * Represents a player exploring a region or structure.
   */
  public static final ActionType EXPLORE = type("explore");

  /**
   * Represents a player eating food.
   */
  public static final ActionType CONSUME = type("eat");

  /**
   * Represents a player collecting a material or item.
   */
  public static final ActionType COLLECT = type("collect");

  /**
   * Represents a player baking a food item.
   */
  public static final ActionType BAKE = type("bake");

  /**
   * Represents a player using or filling a bucket.
   */
  public static final ActionType BUCKET_ENTITY = type("bucket");

  /**
   * Represents a player brushing a block (e.g., for archaeology).
   */
  public static final ActionType BRUSH = type("brush");

  /**
   * Represents a player waxing a copper block.
   */
  public static final ActionType WAX = type("wax");

  /**
   * Represents a player trading with a villager.
   */
  public static final ActionType VILLAGER_TRADE = type("villager_trade");

  private static ActionType type(String keyString) {
    RegistryView<ActionType> registry = RegistryContainer.registryContainer()
        .getRegistry(RegistryKeys.ACTION_TYPES);
    return registry.getOrThrow(new NamespacedKey("jobs", keyString));
  }
}
