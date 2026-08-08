package net.aincraft.service;

import java.util.List;
import java.util.Optional;
import net.aincraft.profession.BlockBreakGate;
import org.jetbrains.annotations.NotNull;

/**
 * Read-only access to the configured profession→material block-break gates.
 */
public interface BlockBreakGateService {

  /** All configured gates, catalog-irrelevant order. */
  @NotNull
  List<BlockBreakGate> gates();

  /** Gate for a material key (case-insensitive), or empty if not gated. */
  @NotNull
  Optional<BlockBreakGate> gateFor(@NotNull String materialKey);
}
