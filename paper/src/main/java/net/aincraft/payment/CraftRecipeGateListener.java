package net.aincraft.payment;

import net.aincraft.profession.RecipeDefinition;
import net.aincraft.service.ProfessionService;
import net.aincraft.service.RecipeService;
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

  public CraftRecipeGateListener(RecipeService recipeService, ProfessionService professionService) {
    this.recipeService = recipeService;
    this.professionService = professionService;
  }

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

    Key recipeKey = Key.key(
        result.getType().getKey().getNamespace(),
        result.getType().getKey().getKey());

    int level = 1;
    var def = recipeService.definition(recipeKey);
    if (def.isPresent()) {
      RecipeDefinition recipe = def.get();
      level = professionService.level(player.getUniqueId(), recipe.professionId()).orElse(0);
    }

    if (!recipeService.canCraft(player.getUniqueId(), recipeKey, level)) {
      event.setCancelled(true);
    }
  }
}
