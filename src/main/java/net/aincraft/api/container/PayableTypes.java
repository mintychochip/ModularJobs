package net.aincraft.api.container;

import org.bukkit.NamespacedKey;

public class PayableTypes {

  private PayableTypes() {
    throw new UnsupportedOperationException();
  }

  public static final PayableType EXPERIENCE = type("experience");

  private static PayableType type(String key) {
    return () -> new NamespacedKey("jobs",key);
  }
}
