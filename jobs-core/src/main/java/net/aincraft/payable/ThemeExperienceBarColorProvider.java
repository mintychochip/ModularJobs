package net.aincraft.payable;

import dev.mintychochip.mint.Mint;
import dev.mintychochip.mint.theme.ColorRole;
import dev.mintychochip.mint.theme.Theme;
import dev.mintychochip.mint.theme.ThemeService;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

/**
 * ExperienceBarColorProvider that uses Mint's theme system to determine the bar color.
 * Maps theme colors to the nearest BossBar.Color.
 */
public final class ThemeExperienceBarColorProvider implements ExperienceBarColorProvider {

  // Use Mint's existing "primary" color role for the experience bar
  private static final ColorRole EXPERIENCE_BAR_ROLE = new ColorRole("primary");

  @Override
  public Color getColor(Player player) {
    if (!Mint.THEME_SERVICE.isLoaded()) {
      return Color.BLUE; // fallback if theme service not available
    }

    ThemeService themeService = Mint.THEME_SERVICE.get();
    Theme theme = themeService.getThemeFor(player);
    TextColor themeColor = theme.getColor(EXPERIENCE_BAR_ROLE);

    return mapTextColorToBossBarColor(themeColor);
  }

  /**
   * Maps a TextColor to the nearest BossBar.Color.
   * BossBar supports: BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW
   */
  private Color mapTextColorToBossBarColor(TextColor textColor) {
    if (textColor == null) {
      return Color.BLUE;
    }

    // Get RGB values from the theme color
    int red = textColor.red();
    int green = textColor.green();
    int blue = textColor.blue();

    // Find the nearest BossBar color using Euclidean distance in RGB space
    Color nearest = Color.BLUE;
    double minDistance = Double.MAX_VALUE;

    for (Color bossBarColor : Color.values()) {
      double distance = colorDistance(red, green, blue, bossBarColor);
      if (distance < minDistance) {
        minDistance = distance;
        nearest = bossBarColor;
      }
    }

    return nearest;
  }

  /**
   * Calculates the Euclidean distance between two RGB colors.
   */
  private double colorDistance(int r1, int g1, int b1, Color bossBarColor) {
    int r2, g2, b2;
    switch (bossBarColor) {
      case BLUE:
        r2 = 85; g2 = 85; b2 = 255;
        break;
      case GREEN:
        r2 = 85; g2 = 255; b2 = 85;
        break;
      case PINK:
        r2 = 255; g2 = 85; b2 = 255;
        break;
      case PURPLE:
        r2 = 170; g2 = 0; b2 = 170;
        break;
      case RED:
        r2 = 255; g2 = 85; b2 = 85;
        break;
      case WHITE:
        r2 = 255; g2 = 255; b2 = 255;
        break;
      case YELLOW:
        r2 = 255; g2 = 255; b2 = 85;
        break;
      default:
        r2 = 0; g2 = 0; b2 = 0;
    }

    int dr = r1 - r2;
    int dg = g1 - g2;
    int db = b1 - b2;
    return Math.sqrt(dr * dr + dg * dg + db * db);
  }
}
