package net.aincraft.profession;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.aincraft.service.BlockBreakGateService;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable in-memory gate table keyed by lowercase material key.
 */
public final class BlockBreakGateStore implements BlockBreakGateService {

  private final Map<String, BlockBreakGate> byMaterial;

  public BlockBreakGateStore(@NotNull List<BlockBreakGate> gates) {
    Map<String, BlockBreakGate> map = gates.stream()
        .collect(Collectors.toUnmodifiableMap(
            g -> g.materialKey().toLowerCase(Locale.ROOT), g -> g));
    this.byMaterial = Map.copyOf(map);
  }

  public boolean isEmpty() {
    return byMaterial.isEmpty();
  }

  @Override
  public @NotNull List<BlockBreakGate> gates() {
    return List.copyOf(byMaterial.values());
  }

  @Override
  public @NotNull Optional<BlockBreakGate> gateFor(@NotNull String materialKey) {
    return Optional.ofNullable(byMaterial.get(materialKey.toLowerCase(Locale.ROOT)));
  }
}
