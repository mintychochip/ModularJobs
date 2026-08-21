package dev.mintychochip.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Drives shipped {@link DurationParser} parse/format for short and documented long-form units. */
class DurationParserTest {

  @Test
  void parseHourAndMinutes() {
    Duration d = DurationParser.parse("1h30m");
    assertEquals(Duration.ofHours(1).plusMinutes(30), d);
  }

  @Test
  void parseDaysHoursMinutesSeconds() {
    Duration d = DurationParser.parse("2d");
    assertEquals(Duration.ofDays(2), d);

    Duration seconds = DurationParser.parse("45s");
    assertEquals(Duration.ofSeconds(45), seconds);
  }

  @Test
  void parseSpacedComposite() {
    Duration d = DurationParser.parse("1h 30m 15s");
    assertEquals(Duration.ofHours(1).plusMinutes(30).plusSeconds(15), d);
  }

  @Test
  void parseDocumentedLongFormUnits() {
    assertEquals(Duration.ofHours(1).plusMinutes(30), DurationParser.parse("1 hour 30 minutes"));
    assertEquals(Duration.ofDays(2), DurationParser.parse("2 days"));
    assertEquals(Duration.ofDays(1), DurationParser.parse("1 day"));
    assertEquals(Duration.ofMinutes(1), DurationParser.parse("1 minute"));
    assertEquals(Duration.ofMinutes(1), DurationParser.parse("1 min"));
    assertEquals(Duration.ofSeconds(30), DurationParser.parse("30 seconds"));
    assertEquals(Duration.ofSeconds(15), DurationParser.parse("15 sec"));
    assertEquals(Duration.ofHours(2), DurationParser.parse("2 hours"));
    assertEquals(Duration.ofHours(1), DurationParser.parse("1 hour"));
  }

  @Test
  void parseLongFormIsCaseInsensitive() {
    assertEquals(Duration.ofHours(1).plusMinutes(5), DurationParser.parse("1 HOUR 5 MINUTES"));
  }

  @Test
  void parseRejectsNullAndBlank() {
    assertThrows(IllegalArgumentException.class, () -> DurationParser.parse(null));
    assertThrows(IllegalArgumentException.class, () -> DurationParser.parse(""));
    assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("   "));
  }

  @Test
  void parseRejectsGarbage() {
    assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("not-a-duration"));
  }

  @Test
  void formatProducesHumanReadableUnits() {
    String formatted =
        DurationParser.format(Duration.ofDays(1).plusHours(2).plusMinutes(5).plusSeconds(3));
    assertTrue(formatted.contains("1d"), "got " + formatted);
    assertTrue(formatted.contains("2h"), "got " + formatted);
    assertTrue(formatted.contains("5m"), "got " + formatted);
    assertTrue(formatted.contains("3s"), "got " + formatted);
  }

  @Test
  void formatNullIsPermanent() {
    assertEquals("Permanent", DurationParser.format(null));
  }

  @Test
  void formatZeroOrNegativeIsZeroSeconds() {
    assertEquals("0s", DurationParser.format(Duration.ZERO));
    assertEquals("0s", DurationParser.format(Duration.ofSeconds(-5)));
  }

  @Test
  void formatRemainingExpiredWhenPast() {
    long started = System.currentTimeMillis() - 60_000L;
    String remaining = DurationParser.formatRemaining(started, Duration.ofSeconds(10));
    assertEquals("Expired", remaining);
  }

  @Test
  void formatRemainingPermanentWhenNullDuration() {
    assertEquals("Permanent", DurationParser.formatRemaining(System.currentTimeMillis(), null));
  }

  @Test
  void formatRemainingStillActive() {
    long started = System.currentTimeMillis();
    String remaining = DurationParser.formatRemaining(started, Duration.ofMinutes(30));
    assertTrue(
        remaining.contains("m") || remaining.contains("s") || remaining.contains("h"),
        "expected remaining time fragment, got " + remaining);
  }
}
