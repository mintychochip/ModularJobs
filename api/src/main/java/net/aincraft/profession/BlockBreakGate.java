package net.aincraft.profession;

import org.jetbrains.annotations.NotNull;

/**
 * One profession→material level gate: breaking the material requires at least
 * {@code minLevel} in {@code professionId} (a §8.1 catalog id).
 *
 * @param materialKey lowercase Minecraft material key, e.g. {@code diamond_ore}
 * @param professionId canonical profession id, e.g. {@code mining}
 * @param minLevel     minimum profession level required to break
 */
public record BlockBreakGate(
    @NotNull String materialKey,
    @NotNull String professionId,
    int minLevel
) {

  public BlockBreakGate {
    materialKey = materialKey.toLowerCase();
    professionId = professionId.toLowerCase();
  }
}
