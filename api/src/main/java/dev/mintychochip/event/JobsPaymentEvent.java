package dev.mintychochip.event;

import java.util.Objects;
import java.util.UUID;
import dev.mintychochip.container.Payable;

/**
 * Fired when a jobs payment is delivered.
 */
public final class JobsPaymentEvent implements Cancellable {

  private final UUID playerId;
  private final Payable base;
  private boolean cancelled;

  public JobsPaymentEvent(UUID playerId, Payable base) {
    this.playerId = Objects.requireNonNull(playerId, "playerId");
    this.base = base;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public Payable getBase() {
    return base;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }
}
