package dev.mintychochip.gui.craftux;

import dev.craftux.api.inventory.InventoryAction;
import dev.craftux.api.inventory.InventoryClick;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Host-owned inventory action registry for craftux {@link dev.craftux.common.inventory.InventoryRuntime}.
 *
 * <p>Runtime freezes the action map at construction; this bus exposes stable proxy
 * handlers for known action ids while GUIs register/unregister live handlers after
 * composition-root wiring.
 */
public final class CraftuxActionBus {

  private final Map<String, InventoryAction> handlers = new ConcurrentHashMap<>();
  private final Map<String, InventoryAction> proxies;

  public CraftuxActionBus(Iterable<String> actionIds) {
    Map<String, InventoryAction> built = new LinkedHashMap<>();
    for (String id : actionIds) {
      Objects.requireNonNull(id, "action id");
      if (id.isBlank()) {
        throw new IllegalArgumentException("action id must not be blank");
      }
      built.put(id, proxy(id));
    }
    this.proxies = Map.copyOf(built);
  }

  /** Immutable proxy map for {@code InventoryRuntime} construction. */
  public Map<String, InventoryAction> proxies() {
    return proxies;
  }

  /** Registers or replaces the live handler for a known action id. */
  public void register(String actionId, InventoryAction handler) {
    if (!proxies.containsKey(actionId)) {
      throw new IllegalArgumentException("unknown action id '" + actionId + "'");
    }
    handlers.put(actionId, Objects.requireNonNull(handler, "handler"));
  }

  private InventoryAction proxy(String actionId) {
    return (UUID audience, InventoryClick click) -> {
      InventoryAction handler = handlers.get(actionId);
      if (handler == null) {
        throw new IllegalStateException("no handler registered for action '" + actionId + "'");
      }
      handler.invoke(audience, click);
    };
  }
}
