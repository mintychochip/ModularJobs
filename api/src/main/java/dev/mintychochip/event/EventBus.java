package dev.mintychochip.event;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simple synchronous pure event bus. Listeners receive every published event and filter by type.
 */
public final class EventBus {

  private final List<Consumer<Object>> listeners = new CopyOnWriteArrayList<>();

  /** Subscribe. */
  public void subscribe(Consumer<Object> listener) {
    listeners.add(Objects.requireNonNull(listener, "listener"));
  }

  /** Publish. */
  public <T> T publish(T event) {
    Objects.requireNonNull(event, "event");
    for (Consumer<Object> listener : listeners) {
      listener.accept(event);
    }
    return event;
  }
}
