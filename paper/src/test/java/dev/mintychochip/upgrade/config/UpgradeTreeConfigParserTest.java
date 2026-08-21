package dev.mintychochip.upgrade.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.boost.BoostFactoryImpl;
import dev.mintychochip.upgrade.UpgradeEffect.BoostEffect;
import dev.mintychochip.upgrade.UpgradeEffect.PermissionEffect;
import dev.mintychochip.upgrade.UpgradeNode;
import dev.mintychochip.upgrade.UpgradeTree;
import dev.mintychochip.upgrade.config.UpgradeTreeConfig.EffectConfig;
import dev.mintychochip.upgrade.config.UpgradeTreeConfig.NodeConfig;
import dev.mintychochip.upgrade.config.UpgradeTreeConfig.PositionConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Drives shipped {@link UpgradeTreeConfigParser} with a minimal legacy tree config. */
class UpgradeTreeConfigParserTest {

  private UpgradeTreeConfigParser parser;

  @BeforeEach
  void setUp() {
    BoostFactoryImpl factory = BoostFactoryImpl.INSTANCE;
    parser = new UpgradeTreeConfigParser(factory, factory);
  }

  @Test
  void parseBuildsTreeWithBoostAndPermissionEffects() {
    UpgradeTreeConfig config =
        new UpgradeTreeConfig(
            "miner",
            "Miner upgrades",
            2,
            "root",
            Map.of(
                "root",
                    new NodeConfig(
                        "Root",
                        "Starting node",
                        "DIAMOND_PICKAXE",
                        0,
                        List.of(),
                        List.of(),
                        List.of("speed"),
                        List.of(
                            new EffectConfig(
                                "permission",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "modularjobs.miner.root",
                                null,
                                null)),
                        new PositionConfig(0, 0),
                        "root_perk",
                        1),
                "speed",
                    new NodeConfig(
                        "Speed Boost",
                        "Mine faster",
                        "SUGAR",
                        3,
                        List.of("root"),
                        List.of(),
                        List.of(),
                        List.of(
                            new EffectConfig(
                                "boost",
                                "experience",
                                1.25,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null)),
                        new PositionConfig(1, 0),
                        "speed_perk",
                        2)));

    UpgradeTree tree = parser.parse(config);

    assertNotNull(tree);
    assertEquals("miner", tree.jobKey());
    assertEquals(2, tree.skillPointsPerLevel());
    assertEquals("root", tree.rootNodeKey());
    assertEquals(2, tree.allNodes().size());

    UpgradeNode root = tree.getNode("root").orElseThrow();
    assertEquals("Root", root.name());
    assertEquals(0, root.cost());
    assertTrue(root.children().contains("speed"));
    assertFalse(root.effects().isEmpty());
    assertTrue(
        root.effects().stream().anyMatch(e -> e instanceof PermissionEffect),
        "root should carry permission effect");

    UpgradeNode speed = tree.getNode("speed").orElseThrow();
    assertEquals(3, speed.cost());
    assertTrue(speed.prerequisites().contains("root"));
    assertTrue(
        speed.effects().stream().anyMatch(e -> e instanceof BoostEffect),
        "speed node should carry boost effect; effects=" + speed.effects());

    BoostEffect boostEffect =
        speed.effects().stream()
            .filter(BoostEffect.class::isInstance)
            .map(BoostEffect.class::cast)
            .findFirst()
            .orElseThrow();
    assertEquals(0, java.math.BigDecimal.valueOf(1.25).compareTo(boostEffect.multiplier()));
  }
}
