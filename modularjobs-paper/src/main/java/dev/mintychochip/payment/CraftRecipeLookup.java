package dev.mintychochip.payment;

import dev.mintychochip.profession.RecipeDefinition;
import dev.mintychochip.service.RecipeService;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Shared craft-output → registered recipe resolution for gate and payment paths. */
public final class CraftRecipeLookup {

  private CraftRecipeLookup() {}

  /** Parses a payment {@code ItemContext} material key into an Adventure output key. */
  public static @NotNull Key outputKeyFromMaterialKey(@NotNull String materialKey) {
    int colon = materialKey.indexOf(':');
    if (colon < 0) {
      return Key.key("minecraft", materialKey);
    }
    return Key.key(materialKey.substring(0, colon), materialKey.substring(colon + 1));
  }

  /** Resolves the crafted result key from a Bukkit item stack. */
  public static @NotNull Key outputKeyFromItemStack(@NotNull ItemStack stack) {
    return Key.key(stack.getType().getKey().getNamespace(), stack.getType().getKey().getKey());
  }

  /** Looks up a registered recipe for the crafted output item. */
  public static Optional<RecipeDefinition> definitionForCraftOutput(
      @NotNull RecipeService recipes, @NotNull Key outputMaterialKey) {
    return recipes.definitionForCraftOutput(outputMaterialKey);
  }
}
