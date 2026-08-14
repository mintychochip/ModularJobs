package net.aincraft.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class ApiUpgradeContractTest {

  @Test
  void capabilityEffectValidatesSchemaAndCopiesPayload() {
    Map<String, String> payload = new java.util.HashMap<>();
    payload.put("mode", "fast");

    NodeEffect.CapabilityEffect effect = new NodeEffect.CapabilityEffect(
        Key.key("modularjobs:example"), 1, payload);

    payload.put("mode", "changed");
    assertEquals(Map.of("mode", "fast"), effect.payload());
    assertThrows(IllegalArgumentException.class,
        () -> new NodeEffect.CapabilityEffect(Key.key("modularjobs:example"), 0, Map.of()));
  }
  @Test
  void purchaseContractUsesCurrentServiceArguments() {
    UpgradeService service = new UpgradeService() {
      @Override
      public java.util.Optional<UpgradeTree> getTree(String jobKey) {
        return java.util.Optional.empty();
      }

      @Override
      public java.util.Optional<SkillTree> getSkillTree(String jobKey) {
        return java.util.Optional.empty();
      }

      @Override
      public java.util.Collection<UpgradeTree> getAllTrees() {
        return java.util.List.of();
      }

      @Override
      public PlayerUpgradeData getPlayerData(String playerId, String jobKey) {
        return null;
      }

      @Override
      public java.util.Set<UpgradeNode> getAvailableNodes(String playerId, String jobKey) {
        return java.util.Set.of();
      }

      @Override
      public UnlockResult unlock(String playerId, String jobKey, String nodeKey) {
        return new UnlockResult.TreeNotFound(jobKey);
      }

      @Override
      public void awardSkillPoints(String playerId, String jobKey, int points) {
      }

      @Override
      public boolean resetUpgrades(String playerId, String jobKey) {
        return false;
      }

      @Override
      public SkillTreeState getSkillTreeState(String playerId, String jobKey) {
        return SkillTreeState.empty(playerId, jobKey);
      }

      @Override
      public PurchaseResult purchaseSkillLevel(String playerId, String jobKey, String nodeKey) {
        return new PurchaseResult.TreeNotFound(jobKey);
      }

      @Override
      public PurchaseResult purchaseMajor(String playerId, String jobKey, String nodeKey) {
        return new PurchaseResult.TreeNotFound(jobKey);
      }

      @Override
      public boolean resetTree(String playerId, String jobKey) {
        return false;
      }

      @Override
      public void clearTreeState(String playerId, String jobKey) {
      }
    };

    assertEquals(false, service.resetTree("player", "job"));
  }
}
