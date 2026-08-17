package net.aincraft.util;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses duration strings like "1h30m", "30s", "2d", "1h 30m 15s".
 * Handles spaces, case-insensitive input, and common unit suffixes.
 */
public final class DurationParser {

  /**
   * Matches optional space-separated unit segments: number + unit letter/word.
   * Longest unit tokens first so "1 hour" / "30 minutes" are not truncated by single letters.
   * Units: days?/day, hours?/hour, minutes?/minute, min, seconds?/second, sec, d, h, m, s.
   */
  private static final Pattern SEGMENT = Pattern.compile(
      "(\\d+)\\s*(days?|hours?|minutes?|seconds?|min|sec|d|h|m|s)",
      Pattern.CASE_INSENSITIVE
  );

  /** Prevents instantiation of this static utility class. */
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

    String normalized = input.trim().toLowerCase(Locale.ROOT);
    Matcher matcher = SEGMENT.matcher(normalized);
    long totalSeconds = 0;
    int matchedEnd = 0;
    boolean found = false;

    while (matcher.find()) {
      // Reject garbage between segments (allow only whitespace)
      String between = normalized.substring(matchedEnd, matcher.start()).trim();
      if (!between.isEmpty()) {
        throw new IllegalArgumentException("Invalid duration format: " + input);
      }
      found = true;
      long value = Long.parseLong(matcher.group(1));
      String unit = matcher.group(2).toLowerCase(Locale.ROOT);
      totalSeconds += switch (unit) {
        case "d", "day", "days" -> value * 86_400L;
        case "h", "hour", "hours" -> value * 3_600L;
        case "m", "min", "minute", "minutes" -> value * 60L;
        case "s", "sec", "second", "seconds" -> value;
        default -> throw new IllegalArgumentException("Invalid duration format: " + input);
      };
      matchedEnd = matcher.end();
    }

    if (!found || !normalized.substring(matchedEnd).trim().isEmpty()) {
      throw new IllegalArgumentException("Invalid duration format: " + input);
    }

    return Duration.ofSeconds(totalSeconds);
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
