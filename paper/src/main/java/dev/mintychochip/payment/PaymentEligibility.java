package dev.mintychochip.payment;

import java.util.Objects;
import java.util.function.Predicate;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Settings-driven payment gate. A {@code true} result from {@link #blocksPay(Player)} means the
 * player must not receive job pay for the current action.
 */
public final class PaymentEligibility {

  private static final String CITIZENS_METADATA = "NPC";

  private final PaymentSettings settings;

  /**
   * Creates the gate backed by the given settings snapshot.
   */
  public PaymentEligibility(@NotNull PaymentSettings settings) {
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  /**
   * Returns the settings driving this payment gate.
   *
   * @return the settings driving this payment gate
   */
  public PaymentSettings settings() {
    return settings;
  }

  /**
   * Reports whether pay must be skipped for this player.
   *
   * @return true when pay must be skipped for this player
   */
  public boolean blocksPay(@NotNull Player player) {
    if (settings.isWorldDisabled(player.getWorld().getName())) {
      return true;
    }
    if (!settings.payInCreative() && player.getGameMode() == GameMode.CREATIVE) {
      return true;
    }
    if (!settings.payWhileRiding() && player.isInsideVehicle()) {
      return true;
    }
    if (player.getGameMode() == GameMode.ADVENTURE) {
      return true;
    }
    return player.hasMetadata(CITIZENS_METADATA);
  }

  /**
   * Predicate form used by existing listener call sites ({@code true} = block pay).
   */
  public Predicate<Player> asPredicate() {
    return this::blocksPay;
  }
}
