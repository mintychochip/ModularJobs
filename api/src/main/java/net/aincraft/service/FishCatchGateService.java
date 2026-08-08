package net.aincraft.service;

import java.util.List;
import java.util.Optional;
import net.aincraft.profession.FishCatchGate;
import org.jetbrains.annotations.NotNull;

/**
 * Read-only access to configured profession-to-fish catch gates.
 */
public interface FishCatchGateService {

  /** All configured gates, catalog-irrelevant order. */
  @NotNull
  List<FishCatchGate> gates();

  /** Gate for an item key (case-insensitive), or empty if not gated. */
  @NotNull
  Optional<FishCatchGate> gateFor(@NotNull String itemKey);
}
