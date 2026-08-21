package dev.mintychochip.event;

/**
 * Pure cancel contract for domain events (no Bukkit dependency).
 */
public interface Cancellable {

  boolean isCancelled();

  void setCancelled(boolean cancelled);
}
