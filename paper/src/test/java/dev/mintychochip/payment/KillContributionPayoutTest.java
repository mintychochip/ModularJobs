package dev.mintychochip.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import dev.mintychochip.test.MockBukkitSupport;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

/**
 * Proves multi-damage kill pay selects each qualifying contributor (not the killer once per row).
 */
class KillContributionPayoutTest {

  @BeforeEach
  void setUp() {
    MockBukkitSupport.mockServer();
  }

  @AfterEach
  void tearDown() {
    MockBukkitSupport.unmockServer();
  }

  @Test
  void selectPayeesReturnsContributorsAboveCutoffNotKillerOnly() {
    PlayerMock alice = MockBukkitSupport.mockServer().addPlayer("alice");
    PlayerMock bob = MockBukkitSupport.mockServer().addPlayer("bob");
    PlayerMock killer = MockBukkitSupport.mockServer().addPlayer("killer");

    DamageContribution contribution = new DamageContribution();
    contribution.addContribution(alice, 60.0);
    contribution.addContribution(bob, 40.0);
    // killer may also be in the map with low damage
    contribution.addContribution(killer, 5.0);

    PaymentEligibility eligibility =
        new PaymentEligibility(PaymentSettings.defaults());

    List<Player> payees = KillContributionPayout.selectPayees(contribution, 0.5, eligibility);
    // alice 60/105 ≈ 0.57 > 0.5; bob ≈ 0.38; killer ≈ 0.05
    assertEquals(1, payees.size());
    assertEquals(alice.getUniqueId(), payees.getFirst().getUniqueId());
  }

  @Test
  void selectPayeesIncludesMultipleQualifyingContributors() {
    PlayerMock alice = MockBukkitSupport.mockServer().addPlayer("alice2");
    PlayerMock bob = MockBukkitSupport.mockServer().addPlayer("bob2");

    DamageContribution contribution = new DamageContribution();
    contribution.addContribution(alice, 55.0);
    contribution.addContribution(bob, 45.0);

    PaymentEligibility eligibility =
        new PaymentEligibility(new PaymentSettings(true, true, Set.of(), 0.4, 25.0));

    List<Player> payees = KillContributionPayout.selectPayees(contribution, 0.4, eligibility);
    assertEquals(2, payees.size());
    assertTrue(payees.stream().anyMatch(p -> p.getUniqueId().equals(alice.getUniqueId())));
    assertTrue(payees.stream().anyMatch(p -> p.getUniqueId().equals(bob.getUniqueId())));
  }
}
