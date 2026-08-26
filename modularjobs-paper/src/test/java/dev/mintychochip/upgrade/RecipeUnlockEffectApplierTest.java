package dev.mintychochip.upgrade;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.profession.RecipeDefinition;
import dev.mintychochip.service.RecipeService;
import dev.mintychochip.upgrade.NodeEffect.RecipeUnlockEffect;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/** Drives shipped {@link UpgradeEffectApplier#syncEffects} recipe_unlock → RecipeService.grant. */
class RecipeUnlockEffectApplierTest {

  @Test
  void syncGrantsRecipeUnlockEffect() {
    ServerMock server = MockBukkit.mock();
    try {
      PlayerMock player = server.addPlayer();
      UUID id = player.getUniqueId();
      RecordingRecipeService recipes = new RecordingRecipeService();

      Key recipe = Key.key("minecraft", "stone_pickaxe");
      SkillNode skill =
          new SkillNode(
              Key.key("miner", "craft_pick"),
              "Craft Pick",
              null,
              "IRON_PICKAXE",
              "IRON_PICKAXE",
              null,
              null,
              SkillNodeKind.SKILL,
              0,
              1,
              SkillNode.LevelEffectMode.CUMULATIVE,
              List.of(new NodeLevel(1, List.of(new RecipeUnlockEffect(recipe)))),
              List.of(),
              Set.of(),
              Set.of(),
              List.of(),
              null,
              List.of(),
              List.of());
      SkillTree tree =
          new SkillTree(
              Key.key("modularjobs", "upgrade_tree/miner"),
              "miner",
              null,
              1,
              "craft_pick",
              Map.of("craft_pick", skill));

      SkillTreeState before = SkillTreeState.empty(id.toString(), "miner");
      SkillTreeState after =
          new SkillTreeState(id.toString(), "miner", 1, Map.of("craft_pick", 1), Map.of());

      UpgradePermissionManager permissions =
          new UpgradePermissionManager(MockBukkit.createMockPlugin("ModularJobs"));
      UpgradeEffectApplier applier = new UpgradeEffectApplier(permissions, recipes);
      applier.syncEffects(player, before, after, tree);

      assertTrue(recipes.granted.contains(recipe), "recipe should be granted on purchase sync");
    } finally {
      MockBukkit.unmock();
    }
  }

  private static final class RecordingRecipeService implements RecipeService {
    final Set<Key> granted = new HashSet<>();

    @Override
    public boolean knows(UUID playerId, Key recipeId) {
      return granted.contains(recipeId);
    }

    @Override
    public void grant(UUID playerId, Key recipeId) {
      granted.add(recipeId);
    }

    @Override
    public void revoke(UUID playerId, Key recipeId) {
      granted.remove(recipeId);
    }

    @Override
    public Set<Key> knownRecipes(UUID playerId) {
      return Set.copyOf(granted);
    }

    @Override
    public void registerDefinition(RecipeDefinition definition) {}

    @Override
    public Optional<RecipeDefinition> definition(Key recipeId) {
      return Optional.empty();
    }

    @Override
    public boolean canCraft(UUID playerId, Key recipeId, int professionLevel) {
      return knows(playerId, recipeId);
    }
  }
}
