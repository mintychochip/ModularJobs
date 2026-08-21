package dev.mintychochip.service;

import dev.mintychochip.profession.ProfessionDefinition;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/** Public profession API over job progression and catalog. */
public interface ProfessionService {

  /** Built-in tracks in catalog order. */
  @NotNull
  List<ProfessionDefinition> tracks();

  /** Resolve. */
  Optional<ProfessionDefinition> resolve(String idOrAlias);

  /** Level. */
  OptionalInt level(@NotNull UUID playerId, @NotNull String professionIdOrAlias);

  /** Experience. */
  Optional<BigDecimal> experience(@NotNull UUID playerId, @NotNull String professionIdOrAlias);

  /**
   * Ensure the player has a progression row for this profession (join if missing).
   *
   * @return true if joined or already present
   */
  boolean ensureTrack(@NotNull UUID playerId, @NotNull String professionIdOrAlias);
}
