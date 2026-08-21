package dev.mintychochip.indirection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import dev.mintychochip.domain.JobResolver;
import dev.mintychochip.service.ItemBoostDataService;
import dev.mintychochip.upgrade.PlayerUpgradeData;
import dev.mintychochip.upgrade.PlayerUpgradeDataImpl;
import dev.mintychochip.upgrade.UpgradeBoostDataService;
import dev.mintychochip.upgrade.UpgradeBoostDataServiceImpl;
import dev.mintychochip.upgrade.UpgradeService;
import dev.mintychochip.upgrade.UpgradeServiceImpl;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * Structural proof that single-implementor interface collapses landed on concrete types (no
 * parallel interface + *Impl pair for these removals), except upgrade contracts which intentionally
 * keep an API interface + paper impl for the v2 skill-tree dual path.
 */
class SingleImplementorCollapseTest {

  @Test
  void collapsedTypesAreConcreteClassesNotInterfaces() throws ClassNotFoundException {
    assertConcrete(JobResolver.class);
    assertConcrete(ItemBoostDataService.class);
    // package-private payable helper — load by name
    assertConcrete(Class.forName("dev.mintychochip.payable.ExperienceBarColorProvider"));
  }

  @Test
  void upgradeContractsKeepInterfacePlusImpl() {
    assertTrue(UpgradeService.class.isInterface());
    assertTrue(UpgradeBoostDataService.class.isInterface());
    assertTrue(PlayerUpgradeData.class.isInterface());
    assertConcrete(UpgradeServiceImpl.class);
    assertConcrete(UpgradeBoostDataServiceImpl.class);
    assertConcrete(PlayerUpgradeDataImpl.class);
  }

  @Test
  void removedParallelTypesNoLongerExist() {
    // FQCNs deleted (interface moved package or *Impl renamed away)
    assertClassMissing("dev.mintychochip.service.JobResolver");
    assertClassMissing("dev.mintychochip.container.boost.ItemBoostDataService");
    assertClassMissing("dev.mintychochip.config.FileBackedConfiguration");
    assertClassMissing("dev.mintychochip.payable.DefaultExperienceBarColorProvider");
    assertClassMissing("dev.mintychochip.domain.JobResolverImpl");
    assertClassMissing("dev.mintychochip.service.ItemBoostDataServiceImpl");
    assertClassMissing("dev.mintychochip.service.PetUpgradeService");
    assertClassMissing("dev.mintychochip.service.PetUpgradeServiceImpl");
    assertClassMissing("dev.mintychochip.gui.PetSelectionGui");
    assertClassMissing("dev.mintychochip.hooks.JobPetsHook");
    assertClassMissing("dev.mintychochip.commands.UpgradeCommand");
  }

  @Test
  void multiImplementorAndBridgeContractsRemainInterfaces() throws ClassNotFoundException {
    assertTrue(Class.forName("dev.mintychochip.service.JobService").isInterface());
    assertTrue(Class.forName("dev.mintychochip.service.PreferencesService").isInterface());
    assertTrue(
        Class.forName("dev.mintychochip.container.boost.TimedBoostDataService").isInterface());
    assertTrue(Class.forName("dev.mintychochip.repository.ConnectionSource").isInterface());
    assertTrue(Class.forName("dev.mintychochip.repository.TimedBoostRepository").isInterface());
    assertTrue(
        Class.forName("dev.mintychochip.domain.repository.JobProgressionRepository").isInterface());
    assertTrue(Class.forName("dev.mintychochip.commands.JobsCommand").isInterface());
    assertTrue(Class.forName("dev.mintychochip.Bridge").isInterface());
  }

  private static void assertConcrete(Class<?> type) {
    assertFalse(type.isInterface(), type.getName() + " must not be an interface");
    assertFalse(Modifier.isAbstract(type.getModifiers()), type.getName() + " must not be abstract");
  }

  private static void assertClassMissing(String fqcn) {
    try {
      Class.forName(fqcn);
      fail("expected " + fqcn + " to be removed");
    } catch (ClassNotFoundException expected) {
      // good
    }
  }
}
