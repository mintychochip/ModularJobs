package net.aincraft.payment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.aincraft.container.ActionTypes;
import net.aincraft.paper.BukkitContexts;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Selects kill payees from multi-damage tracking. Each qualifying contributor is paid, not the
 * final killer once per contributor.
 */
public final class KillContributionPayout {

  private KillContributionPayout() {}

  /**
   * @return players who should receive kill pay (normalized contribution &gt; cutoff, not blocked)
   */
  public static List<Player> selectPayees(
      @NotNull DamageContribution damageContribution,
      double cutoff,
      @NotNull PaymentEligibility eligibility) {
    List<Player> payees = new ArrayList<>();
    Collection<Entity> contributors = damageContribution.getContributors();
    for (Entity contributor : contributors) {
      if (contributor instanceof Player contributorPlayer) {
        double normalized = damageContribution.getContribution(contributor, true);
        if (normalized > cutoff && !eligibility.blocksPay(contributorPlayer)) {
          payees.add(contributorPlayer);
        }
      }
    }
    return payees;
  }

  /**
   * Pays every qualifying kill contributor (normalized contribution above {@code cutoff} and not
   * blocked by {@code eligibility}) via {@code paymentHandler} using the KILL action.
   */
  public static void payContributors(
      @NotNull DamageContribution damageContribution,
      double cutoff,
      @NotNull PaymentEligibility eligibility,
      @NotNull JobsPaymentHandler paymentHandler,
      @NotNull LivingEntity victim) {
    for (Player payee : selectPayees(damageContribution, cutoff, eligibility)) {
      paymentHandler.pay(payee, ActionTypes.KILL, BukkitContexts.entity(victim));
    }
  }
}
