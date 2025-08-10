package net.aincraft.api.action;

import java.util.Locale;
import net.aincraft.api.context.Context.DyeContext;
import net.aincraft.api.context.Context.EntityContext;
import net.aincraft.api.context.Context.MaterialContext;
import net.aincraft.api.context.KeyResolver;
import net.aincraft.api.context.KeyResolver.KeyResolvingStrategy;
import net.aincraft.api.registry.RegistryContainer;
import net.aincraft.api.registry.RegistryKeys;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class ActionTypes {

  static {
    KeyResolver.keyResolver().addStrategy(new KeyResolvingStrategy<MaterialContext>() {
      @Override
      public Class<MaterialContext> getType() {
        return MaterialContext.class;
      }

      @Override
      public Key resolve(MaterialContext object) {
        return object.material().getKey();
      }
    });
    KeyResolver.keyResolver().addStrategy(new KeyResolvingStrategy<DyeContext>() {

      @Override
      public Class<DyeContext> getType() {
        return DyeContext.class;
      }

      @Override
      public Key resolve(DyeContext object) {
        return NamespacedKey.minecraft(object.color().name().toLowerCase(Locale.ENGLISH));
      }
    });
    KeyResolver.keyResolver().addStrategy(new KeyResolvingStrategy<EntityContext>() {

      @Override
      public Class<EntityContext> getType() {
        return EntityContext.class;
      }

      @Override
      public Key resolve(EntityContext object) {
        Entity entity = object.entity();
        return NamespacedKey.minecraft(entity.getName().toLowerCase(Locale.ENGLISH));
      }
    });
  }

  /**
   * Represents a player placing a block.
   */
  public static final ActionType BLOCK_PLACE = actionType("block_place");

  /**
   * Represents a player breaking a block.
   */
  public static final ActionType BLOCK_BREAK = actionType("block_break");

  /**
   * Represents a player killing an entity.
   */
  public static final ActionType KILL = actionType("kill");

  /**
   * Represents a player dyeing a sheep or dyeable entity.
   */
  public static final ActionType DYE = actionType("dye");

  /**
   * Represents a player stripping logs with an axe.
   */
  public static final ActionType STRIP_LOG = actionType("strip_log");

  /**
   * Represents a player crafting an item.
   */
  public static final ActionType CRAFT = actionType("craft");

  /**
   * Represents a player catching a fish.
   */
  public static final ActionType FISH = actionType("fish");

  /**
   * Represents a player smelting an item in a furnace.
   */
  public static final ActionType SMELT = actionType("smelt");

  /**
   * Represents a player brewing a potion.
   */
  public static final ActionType BREW = actionType("brew");

  /**
   * Represents a player enchanting an item.
   */
  public static final ActionType ENCHANT = actionType("enchant");

  /**
   * Represents a player repairing an item.
   */
  public static final ActionType REPAIR = actionType("repair");

  /**
   * Represents a player breeding two animals.
   */
  public static final ActionType BREED = actionType("breed");

  /**
   * Represents a player taming an animal.
   */
  public static final ActionType TAME = actionType("tame");

  /**
   * Represents a player shearing an entity such as a sheep.
   */
  public static final ActionType SHEAR = actionType("shear");

  /**
   * Represents a player milking an entity.
   */
  public static final ActionType MILK = actionType("milk");

  /**
   * Represents a player exploring a region or structure.
   */
  public static final ActionType EXPLORE = actionType("explore");

  /**
   * Represents a player eating food.
   */
  public static final ActionType EAT = actionType("eat");

  /**
   * Represents a player collecting a material or item.
   */
  public static final ActionType COLLECT = actionType("collect");

  /**
   * Represents a player baking a food item.
   */
  public static final ActionType BAKE = actionType("bake");

  /**
   * Represents a player using or filling a bucket.
   */
  public static final ActionType BUCKET = actionType("bucket");

  /**
   * Represents a player brushing a block (e.g., for archaeology).
   */
  public static final ActionType BRUSH = actionType("brush");

  /**
   * Represents a player waxing a copper block.
   */
  public static final ActionType WAX = actionType("wax");

  /**
   * Represents a player trading with a villager.
   */
  public static final ActionType VILLAGER_TRADE = actionType("villager_trade");


  private static ActionType actionType(String keyString) {
    Key key = new NamespacedKey("jobs", keyString);
    ActionType type = new ActionType() {

      @Override
      public @NotNull Key key() {
        return key;
      }
    };
    RegistryContainer.registryContainer()
        .editRegistry(RegistryKeys.ACTION_TYPES, r -> r.register(type));
    return type;
  }
}
