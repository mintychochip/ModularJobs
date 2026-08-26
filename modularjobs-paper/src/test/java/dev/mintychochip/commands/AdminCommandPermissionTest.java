package dev.mintychochip.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.mintychochip.test.MockBukkitSupport;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.lang.reflect.Proxy;
import java.util.function.Predicate;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Drives shipped admin permission gate ({@link AdminPermissions} + command {@code build()} trees).
 */
class AdminCommandPermissionTest {

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void permissionConstantsMatchAdminNode() {
    assertEquals(AdminPermissions.ADMIN, BoostCommand.PERMISSION);
    assertEquals(AdminPermissions.ADMIN, EditorCommand.PERMISSION);
    assertEquals(AdminPermissions.ADMIN, ApplyEditsCommand.PERMISSION);
    assertEquals("modularjobs.admin", AdminPermissions.ADMIN);
  }

  @Test
  void adminPermissionsRejectsNonOpAndAllowsOp() {
    PlayerMock denied = MockBukkitSupport.mockServer().addPlayer("denied");
    denied.setOp(false);
    assertFalse(AdminPermissions.isAdmin(denied));
    assertFalse(AdminPermissions.isAdmin(sourceWith(denied)));

    PlayerMock allowed = MockBukkitSupport.mockServer().addPlayer("allowed");
    allowed.setOp(true);
    assertTrue(AdminPermissions.isAdmin(allowed));
    assertTrue(AdminPermissions.isAdmin(sourceWith(allowed)));
  }

  @Test
  void editorCommandBuildRequiresAdmin() {
    EditorCommand command = new EditorCommand(null, null, null);
    assertRequirement(command.build());
  }

  @Test
  void applyEditsCommandBuildRequiresAdmin() {
    ApplyEditsCommand command = new ApplyEditsCommand(null);
    assertRequirement(command.build());
  }

  /**
   * BoostCommand.build() pulls Paper ArgumentTypes (not fully MockBukkit-backed). Assert the same
   * shipped predicate used by its root {@code .requires(AdminPermissions::isAdmin)}.
   */
  @Test
  void boostCommandUsesAdminPermissionsGate() {
    assertEquals(AdminPermissions.ADMIN, BoostCommand.PERMISSION);
    PlayerMock denied = MockBukkitSupport.mockServer().addPlayer("boost-denied");
    denied.setOp(false);
    assertFalse(AdminPermissions.isAdmin(sourceWith(denied)));
    PlayerMock allowed = MockBukkitSupport.mockServer().addPlayer("boost-allowed");
    allowed.setOp(true);
    assertTrue(AdminPermissions.isAdmin(sourceWith(allowed)));
  }

  private static void assertRequirement(LiteralArgumentBuilder<CommandSourceStack> builder) {
    LiteralCommandNode<CommandSourceStack> node = builder.build();
    Predicate<CommandSourceStack> requirement = node.getRequirement();

    PlayerMock denied = MockBukkitSupport.mockServer().addPlayer();
    denied.setOp(false);
    assertFalse(requirement.test(sourceWith(denied)));

    PlayerMock allowed = MockBukkitSupport.mockServer().addPlayer();
    allowed.setOp(true);
    assertTrue(requirement.test(sourceWith(allowed)));
  }

  private static CommandSourceStack sourceWith(CommandSender sender) {
    return (CommandSourceStack)
        Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {CommandSourceStack.class},
            (proxy, method, args) -> {
              if ("getSender".equals(method.getName())) {
                return sender;
              }
              if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
              }
              if ("toString".equals(method.getName())) {
                return "CommandSourceStackProxy(" + sender + ")";
              }
              Class<?> rt = method.getReturnType();
              if (rt == boolean.class) {
                return false;
              }
              if (rt == int.class) {
                return 0;
              }
              if (rt == long.class) {
                return 0L;
              }
              if (rt == double.class) {
                return 0.0d;
              }
              if (rt == float.class) {
                return 0.0f;
              }
              return null;
            });
  }
}
