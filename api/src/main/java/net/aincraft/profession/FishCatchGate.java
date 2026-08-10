package net.aincraft.profession;

import java.util.Locale;
import org.jetbrains.annotations.NotNull;

/**
 * One profession level gate for catching a configured fish item.
 *
 * @param itemKey lowercase Minecraft item key, e.g. {@code salmon}
 * @param professionId canonical profession id, e.g. {@code fishing}
 * @param minLevel minimum profession level required to catch the item
 */
public record FishCatchGate(
    @NotNull String itemKey,
    @NotNull String professionId,
    int minLevel
) {

  public FishCatchGate {
    itemKey = itemKey.toLowerCase(Locale.ROOT);
    professionId = professionId.toLowerCase(Locale.ROOT);
  }
}
