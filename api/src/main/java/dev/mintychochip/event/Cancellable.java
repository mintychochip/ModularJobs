package dev.mintychochip.event;

/** Pure cancel contract for domain events (no Bukkit dependency). */
public interface Cancellable {

  /** Returns whether cancelled. */
  boolean isCancelled();

  /** Sets the cancelled. */
  void setCancelled(boolean cancelled);
}
