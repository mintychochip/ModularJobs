package net.aincraft.payable;

import dev.mintychochip.mint.preferences.PreferenceType;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.bossbar.BossBar.Color;

/**
 * PreferenceType for serializing BossBar.Color enum values.
 */
public final class BossBarColorPreferenceType implements PreferenceType<Color> {

  private static final String KEY = "bossbar_color";

  @Override
  public String key() {
    return KEY;
  }

  @Override
  public byte[] serialize(Color value) {
    return value != null ? value.name().getBytes(StandardCharsets.UTF_8) : null;
  }

  @Override
  public Color deserialize(byte[] value) {
    if (value == null || value.length == 0) {
      return null;
    }
    try {
      String colorName = new String(value, StandardCharsets.UTF_8);
      return Color.valueOf(colorName.toUpperCase());
    } catch (IllegalArgumentException e) {
      return Color.BLUE; // default fallback
    }
  }
}
