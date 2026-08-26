package dev.mintychochip.gui.craftux;

import dev.craftux.api.inventory.ItemSpec;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;

/** Helpers for craftux {@link ItemSpec} construction from Bukkit materials / Adventure text. */
public final class CraftuxItems {

  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();

  private CraftuxItems() {}

  /** Of. */
  public static ItemSpec of(Material material, String label) {
    return of(material, label, List.of());
  }

  /** Of. */
  public static ItemSpec of(Material material, String label, List<String> lore) {
    return new ItemSpec(materialKey(material), 1, nullToEmpty(label), List.copyOf(lore));
  }

  /** Of. */
  public static ItemSpec of(Material material, Component label, List<Component> lore) {
    List<String> lines = new ArrayList<>(lore.size());
    for (Component line : lore) {
      lines.add(PLAIN.serialize(line));
    }
    return new ItemSpec(materialKey(material), 1, PLAIN.serialize(label), lines);
  }

  /** Pane. */
  public static ItemSpec pane(Material material) {
    return of(material, " ");
  }

  /** Material key. */
  public static String materialKey(Material material) {
    return material.getKey().asString();
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
