package net.aincraft.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import net.aincraft.upgrade.SkillTree;
import net.aincraft.upgrade.UpgradeService;
import net.aincraft.upgrade.UpgradeTree;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class UpgradesCommandTest {

  @Test
  void v2OnlyJobHasAnUpgradeTreeForViewCommand() {
    SkillTree minerTree = new SkillTree(
        Key.key("modularjobs", "upgrade_tree/miner"),
        "miner", null, 1, "root", Map.of());
    UpgradeService service = serviceWithSkillTree(minerTree);

    assertTrue(UpgradesCommand.hasTreeForJob(service, "miner"));
  }

  private static UpgradeService serviceWithSkillTree(SkillTree tree) {
    return (UpgradeService) Proxy.newProxyInstance(
        Thread.currentThread().getContextClassLoader(),
        new Class<?>[] {UpgradeService.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getTree" -> Optional.<UpgradeTree>empty();
          case "getSkillTree" -> Optional.of(tree);
          default -> defaultValue(method.getReturnType());
        });
  }

  private static Object defaultValue(Class<?> type) {
    if (type == boolean.class) {
      return false;
    }
    if (type == int.class) {
      return 0;
    }
    if (type == long.class) {
      return 0L;
    }
    if (type == double.class) {
      return 0D;
    }
    if (type == float.class) {
      return 0F;
    }
    return null;
  }
}
