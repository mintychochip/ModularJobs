package net.aincraft.util;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses duration strings like "1h30m", "30s", "2d", "1h 30m 15s".
 */
public final class DurationParser {

  private static final Pattern DURATION_PATTERN = Pattern.compile(
      "(?:(\\d+)d)?\\s*(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?",
      Pattern.CASE_INSENSITIVE
  );

  private DurationParser() {
  }

  /**
   * Parse a duration string into a Duration object.
   *
   * @param input duration string (e.g., "1h30m", "2d", "30s", "1h 30m 15s")
   * @return parsed Duration
   * @throws IllegalArgumentException if the format is invalid
   */
  public static Duration parse(String input) throws IllegalArgumentException {
    if (input == null || input.isBlank()) {
      throw new IllegalArgumentException("Duration string cannot be empty");
    }

    String normalized = input.trim().toLowerCase();
    Matcher matcher = DURATION_PATTERN.matcher(normalized);

    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid duration format: " + input);
    }

    String daysStr = matcher.group(1);
    String hoursStr = matcher.group(2);
    String minutesStr = matcher.group(3);
    String secondsStr = matcher.group(4);

    // Check if at least one component was provided
    if (daysStr == null && hoursStr == null && minutesStr == null && secondsStr == null) {
      throw new IllegalArgumentException("Invalid duration format: " + input);
    }

    long days = daysStr != null ? Long.parseLong(daysStr) : 0;
    long hours = hoursStr != null ? Long.parseLong(hoursStr) : 0;
    long minutes = minutesStr != null ? Long.parseLong(minutesStr) : 0;
    long seconds = secondsStr != null ? Long.parseLong(secondsStr) : 0;

    return Duration.ofDays(days)
        .plusHours(hours)
        .plusMinutes(minutes)
        .plusSeconds(seconds);
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
