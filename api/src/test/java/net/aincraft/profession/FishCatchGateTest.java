package net.aincraft.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.aincraft.service.FishCatchGateService;
import org.junit.jupiter.api.Test;

class FishCatchGateTest {

  private record FixedService(List<FishCatchGate> gates) implements FishCatchGateService {
    @Override
    public List<FishCatchGate> gates() {
      return gates;
    }

    @Override
    public Optional<FishCatchGate> gateFor(String itemKey) {
      return gates.stream()
          .filter(gate -> gate.itemKey().equalsIgnoreCase(itemKey))
          .findFirst();
    }
  }

  @Test
  void recordNormalizesCase() {
    FishCatchGate gate = new FishCatchGate("Tropical_Fish", "Fisherman", 20);

    assertEquals("tropical_fish", gate.itemKey());
    assertEquals("fisherman", gate.professionId());
    assertEquals(20, gate.minLevel());
  }

  @Test
  void serviceFindsGateCaseInsensitively() {
    FishCatchGateService service = new FixedService(
        List.of(new FishCatchGate("cod", "fishing", 1)));

    assertTrue(service.gateFor("COD").isPresent());
    assertEquals(1, service.gateFor("cod").orElseThrow().minLevel());
    assertTrue(service.gateFor("salmon").isEmpty());
  }
}
