package net.aincraft.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

/**
 * Verifies the shipped {@link Messages} themed MiniMessage tags (replacing Mint messaging).
 */
class MessagesTest {

  private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

  /** Verifies null and empty strings produce empty components. */
  @Test
  void componentEmptyAndNullAreEmpty() {
    assertEquals(Component.empty(), Messages.component(null));
    assertEquals(Component.empty(), Messages.component(""));
  }

  /** Verifies theme tags are stripped while preserving visible text. */
  @Test
  void componentStripsThemeTagsAndKeepsPlainText() {
    Component c = Messages.component("<error>Boom</error> <success>ok</success>");
    assertEquals("Boom ok", PLAIN.serialize(c));
  }

  /** Verifies each theme tag maps to the expected named color. */
  @Test
  void componentAppliesNamedThemeColors() {
    Component error = Messages.component("<error>x</error>");
    Component success = Messages.component("<success>y</success>");
    Component primary = Messages.component("<primary>p</primary>");
    final Component secondary = Messages.component("<secondary>s</secondary>");
    final Component neutral = Messages.component("<neutral>n</neutral>");
    final Component accent = Messages.component("<accent>a</accent>");
    final Component info = Messages.component("<info>i</info>");

    assertTrue(hasColor(error, NamedTextColor.RED), "error -> red");
    assertTrue(hasColor(success, NamedTextColor.GREEN), "success -> green");
    assertTrue(hasColor(primary, NamedTextColor.GOLD), "primary -> gold");
    assertTrue(hasColor(secondary, NamedTextColor.YELLOW), "secondary -> yellow");
    assertTrue(hasColor(neutral, NamedTextColor.GRAY), "neutral -> gray");
    assertTrue(hasColor(accent, NamedTextColor.AQUA), "accent -> aqua");
    assertTrue(hasColor(info, NamedTextColor.BLUE), "info -> blue");
  }

  /** Verifies nested theme tags resolve completely in plain text. */
  @Test
  void componentNestedThemeTagsSerializeFully() {
    String plain = PLAIN.serialize(
        Messages.component("<neutral>━━ <primary>Jobs Top</primary> <accent>#1</accent> ━━"));
    assertEquals("━━ Jobs Top #1 ━━", plain);
    assertFalse(plain.contains("<"), "tags must be resolved, not left as literal text");
  }

  /** Returns whether {@code component} or any child uses {@code expected}. */
  private static boolean hasColor(Component component, NamedTextColor expected) {
    if (expected.equals(component.color())) {
      return true;
    }
    for (Component child : component.children()) {
      if (hasColor(child, expected)) {
        return true;
      }
    }
    return false;
  }
}
