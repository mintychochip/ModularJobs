package net.aincraft.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.aincraft.service.BlockBreakGateService;
import org.junit.jupiter.api.Test;

class BlockBreakGateTest {

  private record FixedService(List<BlockBreakGate> gates) implements BlockBreakGateService {
    @Override
    public List<BlockBreakGate> gates() {
      return gates;
    }

    @Override
    public Optional<BlockBreakGate> gateFor(String materialKey) {
      return gates.stream()
          .filter(g -> g.materialKey().equalsIgnoreCase(materialKey))
          .findFirst();
    }
  }

  @Test
  void recordNormalizesCase() {
    BlockBreakGate gate = new BlockBreakGate("Diamond_Ore", "Mining", 30);
    assertEquals("diamond_ore", gate.materialKey());
    assertEquals("mining", gate.professionId());
    assertEquals(30, gate.minLevel());
  }

  @Test
  void serviceFindsGateCaseInsensitively() {
    BlockBreakGateService svc = new FixedService(
        List.of(new BlockBreakGate("diamond_ore", "mining", 30)));
    assertTrue(svc.gateFor("DIAMOND_ORE").isPresent());
    assertEquals(30, svc.gateFor("diamond_ore").orElseThrow().minLevel());
    assertTrue(svc.gateFor("stone").isEmpty());
  }
}
