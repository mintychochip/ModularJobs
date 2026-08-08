package net.aincraft.profession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.aincraft.service.FishCatchGateService;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable in-memory fish catch gate table keyed by lowercase item key.
 */
public final class FishCatchGateStore implements FishCatchGateService {

  private final Map<String, FishCatchGate> byItem;

  public FishCatchGateStore(@NotNull List<FishCatchGate> gates) {
    Map<String, FishCatchGate> map = new LinkedHashMap<>();
    for (FishCatchGate gate : gates) {
      map.putIfAbsent(gate.itemKey().toLowerCase(Locale.ROOT), gate);
    }
    this.byItem = Map.copyOf(map);
  }

  public boolean isEmpty() {
    return byItem.isEmpty();
  }

  @Override
  public @NotNull List<FishCatchGate> gates() {
    return List.copyOf(byItem.values());
  }

  @Override
  public @NotNull Optional<FishCatchGate> gateFor(@NotNull String itemKey) {
    return Optional.ofNullable(byItem.get(itemKey.toLowerCase(Locale.ROOT)));
  }
}
