package net.aincraft.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.aincraft.profession.ProfessionDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * Public profession API over job progression and catalog.
 */
public interface ProfessionService {

  /** Built-in tracks in catalog order. */
  @NotNull
  List<ProfessionDefinition> tracks();

  /** Resolve canonical id, storage key, or legacy alias. */
  Optional<ProfessionDefinition> resolve(String idOrAlias);

  /**
   * Profession level for a player, or empty if no progression row (not joined / no XP yet).
   */
  OptionalInt level(@NotNull UUID playerId, @NotNull String professionIdOrAlias);

  /**
   * Raw experience points, or empty if no progression.
   */
  Optional<BigDecimal> experience(@NotNull UUID playerId, @NotNull String professionIdOrAlias);

  /**
   * Ensure the player has a progression row for this profession (join if missing).
   *
   * @return true if joined or already present
   */
  boolean ensureTrack(@NotNull UUID playerId, @NotNull String professionIdOrAlias);
}
