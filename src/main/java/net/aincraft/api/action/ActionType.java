package net.aincraft.api.action;

import net.aincraft.api.Bridge;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a type of in-game action a {@link org.bukkit.entity.Player} can perform.
 */
@NonExtendable
public sealed interface ActionType extends Keyed permits ActionTypeImpl {

  static ActionType create(@NotNull Key key) {
    return ActionTypeImpl.create(key);
  }

  @Internal
  private static ActionType type(String keyString) {
    return create(new NamespacedKey(Bridge.bridge().plugin(), keyString));
  }

  /**
   * Represents a player placing a block.
   */
  ActionType BLOCK_PLACE = type("block_place");

  /**
   * Represents a player breaking a block.
   */
  ActionType BLOCK_BREAK = type("block_break");

  /**
   * Represents a player killing an entity.
   */
  ActionType KILL = type("kill");

  /**
   * Represents a player dyeing a sheep or dyeable entity.
   */
  ActionType DYE = type("dye");

  /**
   * Represents a player stripping logs with an axe.
   */
  ActionType STRIP_LOG = type("strip_log");

  /**
   * Represents a player crafting an item.
   */
  ActionType CRAFT = type("craft");

  /**
   * Represents a player catching a fish.
   */
  ActionType FISH = type("fish");

  /**
   * Represents a player smelting an item in a furnace.
   */
  ActionType SMELT = type("smelt");

  /**
   * Represents a player brewing a potion.
   */
  ActionType BREW = type("brew");

  /**
   * Represents a player enchanting an item.
   */
  ActionType ENCHANT = type("enchant");

  /**
   * Represents a player repairing an item.
   */
  ActionType REPAIR = type("repair");

  /**
   * Represents a player breeding two animals.
   */
  ActionType BREED = type("breed");

  /**
   * Represents a player taming an animal.
   */
  ActionType TAME = type("tame");

  /**
   * Represents a player shearing an entity such as a sheep.
   */
  ActionType SHEAR = type("shear");

  /**
   * Represents a player milking an entity.
   */
  ActionType MILK = type("milk");

  /**
   * Represents a player exploring a region or structure.
   */
  ActionType EXPLORE = type("explore");

  /**
   * Represents a player eating food.
   */
  ActionType CONSUME = type("eat");

  /**
   * Represents a player collecting a material or item.
   */
  ActionType COLLECT = type("collect");

  /**
   * Represents a player baking a food item.
   */
  ActionType BAKE = type("bake");

  /**
   * Represents a player using or filling a bucket.
   */
  ActionType BUCKET_ENTITY = type("bucket");

  /**
   * Represents a player brushing a block (e.g., for archaeology).
   */
  ActionType BRUSH = type("brush");

  /**
   * Represents a player waxing a copper block.
   */
  ActionType WAX = type("wax");

  /**
   * Represents a player trading with a villager.
   */
  ActionType VILLAGER_TRADE = type("villager_trade");

}
