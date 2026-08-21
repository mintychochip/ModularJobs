package dev.mintychochip.profession;

import dev.mintychochip.JobProgression;
import dev.mintychochip.service.JobService;
import dev.mintychochip.service.ProfessionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/** Facade: §8.1 catalog + {@link JobService} progression (storage keys). */
public final class ProfessionServiceImpl implements ProfessionService {

  private final JobService jobService;

  /** Profession service impl. */
  public ProfessionServiceImpl(JobService jobService) {
    this.jobService = jobService;
  }

  @Override
  public @NotNull List<ProfessionDefinition> tracks() {
    return ProfessionCatalog.tracks();
  }

  @Override
  public Optional<ProfessionDefinition> resolve(String idOrAlias) {
    return ProfessionCatalog.resolve(idOrAlias);
  }

  @Override
  public OptionalInt level(@NotNull UUID playerId, @NotNull String professionIdOrAlias) {
    return progression(playerId, professionIdOrAlias)
        .map(p -> OptionalInt.of(p.level()))
        .orElseGet(OptionalInt::empty);
  }

  @Override
  public Optional<BigDecimal> experience(
      @NotNull UUID playerId, @NotNull String professionIdOrAlias) {
    return progression(playerId, professionIdOrAlias).map(JobProgression::experience);
  }

  @Override
  public boolean ensureTrack(@NotNull UUID playerId, @NotNull String professionIdOrAlias) {
    Optional<ProfessionDefinition> def = ProfessionCatalog.resolve(professionIdOrAlias);
    if (def.isEmpty()) {
      return false;
    }
    String storageKey = def.get().storageKey();
    try {
      JobProgression existing = jobService.getProgression(playerId.toString(), storageKey);
      if (existing != null) {
        return true;
      }
    } catch (IllegalArgumentException ignored) {
      // no progression
    }
    return jobService.joinJob(playerId.toString(), storageKey);
  }

  private Optional<JobProgression> progression(UUID playerId, String professionIdOrAlias) {
    Optional<ProfessionDefinition> def = ProfessionCatalog.resolve(professionIdOrAlias);
    if (def.isEmpty()) {
      return Optional.empty();
    }
    String storageKey = def.get().storageKey();
    JobProgression direct = jobService.getProgression(playerId.toString(), storageKey);
    if (direct != null) {
      return Optional.of(direct);
    }
    return jobService.getProgressions(playerId).stream()
        .filter(p -> p.job().key().value().equalsIgnoreCase(storageKey))
        .findFirst();
  }
}
