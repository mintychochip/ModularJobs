package net.aincraft.indirection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import net.aincraft.domain.JobResolver;
import net.aincraft.service.ItemBoostDataService;
import net.aincraft.service.PetUpgradeService;
import net.aincraft.upgrade.PlayerUpgradeData;
import net.aincraft.upgrade.UpgradeBoostDataService;
import net.aincraft.upgrade.UpgradeService;
import org.junit.jupiter.api.Test;

/**
 * Structural proof that single-implementor interface collapses landed on concrete types
 * (no parallel interface + *Impl pair for these removals).
 */
class SingleImplementorCollapseTest {

  @Test
  void collapsedTypesAreConcreteClassesNotInterfaces() throws ClassNotFoundException {
    assertConcrete(JobResolver.class);
    assertConcrete(ItemBoostDataService.class);
    assertConcrete(PetUpgradeService.class);
    assertConcrete(UpgradeService.class);
    assertConcrete(UpgradeBoostDataService.class);
    assertConcrete(PlayerUpgradeData.class);
    // package-private payable helper — load by name
    assertConcrete(Class.forName("net.aincraft.payable.ExperienceBarColorProvider"));
  }

  @Test
  void removedParallelTypesNoLongerExist() {
    // FQCNs deleted (interface moved package or *Impl renamed away)
    assertClassMissing("net.aincraft.service.JobResolver");
    assertClassMissing("net.aincraft.container.boost.ItemBoostDataService");
    assertClassMissing("net.aincraft.config.FileBackedConfiguration");
    assertClassMissing("net.aincraft.payable.DefaultExperienceBarColorProvider");
    assertClassMissing("net.aincraft.domain.JobResolverImpl");
    assertClassMissing("net.aincraft.service.ItemBoostDataServiceImpl");
    assertClassMissing("net.aincraft.service.PetUpgradeServiceImpl");
    assertClassMissing("net.aincraft.upgrade.UpgradeServiceImpl");
    assertClassMissing("net.aincraft.upgrade.UpgradeBoostDataServiceImpl");
    assertClassMissing("net.aincraft.upgrade.PlayerUpgradeDataImpl");
  }

  @Test
  void multiImplementorAndBridgeContractsRemainInterfaces() throws ClassNotFoundException {
    assertTrue(Class.forName("net.aincraft.service.JobService").isInterface());
    assertTrue(Class.forName("net.aincraft.service.PreferencesService").isInterface());
    assertTrue(Class.forName("net.aincraft.container.boost.TimedBoostDataService").isInterface());
    assertTrue(Class.forName("net.aincraft.repository.ConnectionSource").isInterface());
    assertTrue(Class.forName("net.aincraft.repository.TimedBoostRepository").isInterface());
    assertTrue(Class.forName("net.aincraft.domain.repository.JobProgressionRepository").isInterface());
    assertTrue(Class.forName("net.aincraft.commands.JobsCommand").isInterface());
    assertTrue(Class.forName("net.aincraft.Bridge").isInterface());
  }

  private static void assertConcrete(Class<?> type) {
    assertFalse(type.isInterface(), type.getName() + " must not be an interface");
    assertFalse(Modifier.isAbstract(type.getModifiers()), type.getName() + " must not be abstract");
  }

  private static void assertClassMissing(String fqcn) {
    try {
      Class.forName(fqcn);
      assertFalse(true, "expected " + fqcn + " to be removed");
    } catch (ClassNotFoundException expected) {
      // good
    }
  }
}
