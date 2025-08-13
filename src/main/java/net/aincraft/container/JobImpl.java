package net.aincraft.container;

import java.util.Map;
import net.aincraft.api.Job;
import net.aincraft.api.container.PayableCurve;
import net.aincraft.api.container.PayableType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

public final class JobImpl implements Job {

  private final String key;
  private final Component displayName;
  private final Component description;

  private final Map<PayableType, PayableCurve> payableCurves;

  public JobImpl(String key, Component displayName, Component description, Map<PayableType,PayableCurve> payableCurves) {
    this.key = key;
    this.displayName = displayName;
    this.description = description;
    this.payableCurves = payableCurves;
  }

  @Override
  public Component getDisplayName() {
    return displayName;
  }

  @Override
  public Component getDescription() {
    return description;
  }

  @Override
  public PayableCurve getCurve(PayableType type) {
    return payableCurves.get(type);
  }

  @Override
  public void setCurve(PayableType type, PayableCurve curve) {
    payableCurves.put(type,curve);
  }

  @Override
  public int getMaxLevel() {
    return 200;
  }

  @Override
  public @NotNull Key key() {
    return new NamespacedKey("jobs", key);
  }
}
