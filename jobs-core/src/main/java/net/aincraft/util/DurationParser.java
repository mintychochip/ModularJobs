package net.aincraft.util;

import java.time.Duration;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;

/**
 * Parses duration strings like "1h30m", "30s", "2d", "1h 30m 15s" using Configurate's serializer.
 * Handles spaces, case-insensitive input, and various common formats.
 */
public final class DurationParser {

  private DurationParser() {
  }

  /**
   * Parse a duration string into a Duration object.
   * Accepts formats: "1h30m", "2d", "30s", "1h 30m 15s", "1 hour 30 minutes", etc.
   *
   * @param input duration string
   * @return parsed Duration
   * @throws IllegalArgumentException if the format is invalid
   */
  public static Duration parse(String input) throws IllegalArgumentException {
    if (input == null || input.isBlank()) {
      throw new IllegalArgumentException("Duration string cannot be empty");
    }

    try {
      ConfigurationNode node = BasicConfigurationNode.root();
      node.set(input);
      Duration duration = node.get(Duration.class);
      if (duration == null) {
        throw new IllegalArgumentException("Invalid duration format: " + input);
      }
      return duration;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid duration format: " + input, e);
    }
  }

  /**
   * Format a duration into a human-readable string.
   *
   * @param duration the duration to format
   * @return formatted string (e.g., "1h 30m", "2d 5h")
   */
  public static String format(Duration duration) {
    if (duration == null) {
      return "Permanent";
    }

    long totalSeconds = duration.getSeconds();
    if (totalSeconds <= 0) {
      return "0s";
    }

    long days = totalSeconds / 86400;
    long hours = (totalSeconds % 86400) / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;

    StringBuilder sb = new StringBuilder();
    if (days > 0) {
      sb.append(days).append("d ");
    }
    if (hours > 0) {
      sb.append(hours).append("h ");
    }
    if (minutes > 0) {
      sb.append(minutes).append("m ");
    }
    if (seconds > 0 || sb.isEmpty()) {
      sb.append(seconds).append("s");
    }

    return sb.toString().trim();
  }

  /**
   * Format remaining time from a start time and duration.
   *
   * @param startMillis start time in milliseconds
   * @param duration    the duration
   * @return formatted remaining time string
   */
  public static String formatRemaining(long startMillis, Duration duration) {
    if (duration == null) {
      return "Permanent";
    }

    long expiresAt = startMillis + duration.toMillis();
    long remaining = expiresAt - System.currentTimeMillis();

    if (remaining <= 0) {
      return "Expired";
    }

    return format(Duration.ofMillis(remaining));
  }
}
