package dev.mintychochip.payment;

import dev.mintychochip.profession.RecipeDefinition;
import dev.mintychochip.service.ProfessionService;
import dev.mintychochip.service.RecipeService;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.inventory.InventoryType.SlotType;

/**
 * Blocks crafting of registered unknown recipes according to configured profession rules.
 * Unregistered recipes remain allowed for vanilla BC.
 */
public final class CraftRecipeGateListener implements Listener {

  private final RecipeService recipeService;
  private final ProfessionService professionService;

  /**
   * Creates the gate with the recipe and profession services used to resolve craftability.
   */
  public CraftRecipeGateListener(RecipeService recipeService, ProfessionService professionService) {
    this.recipeService = recipeService;
    this.professionService = professionService;
  }

  /**
   * Cancels crafting when the recipe is registered and the player's profession level does not
   * permit it. Unregistered recipes are always allowed.
   */
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onCraft(CraftItemEvent event) {
    ItemStack result = event.getCurrentItem();
    if (event.getSlotType() != SlotType.RESULT || result == null || result.getType().isAir()) {
      return;
    }
    HumanEntity entity = event.getWhoClicked();
    if (!(entity instanceof Player player)) {
      return;
    }

    Key outputKey = CraftRecipeLookup.outputKeyFromItemStack(result);

    int level = 1;
    Key recipeId = outputKey;
    var def = CraftRecipeLookup.definitionForCraftOutput(recipeService, outputKey);
    if (def.isPresent()) {
      RecipeDefinition recipe = def.get();
      recipeId = recipe.id();
      level = professionService.level(player.getUniqueId(), recipe.professionId()).orElse(0);
    }

    if (!recipeService.canCraft(player.getUniqueId(), recipeId, level)) {
      event.setCancelled(true);
    }
  }
}
