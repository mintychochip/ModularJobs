package net.aincraft.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class EditorConfigTest {

  @Test
  void defaultsDisableExternalEditorUntilConfigured() {
    EditorConfig config = EditorConfig.defaults();

    assertFalse(config.enabled());
    assertEquals("", config.sessionApiUrl());
    assertEquals("", config.webEditorUrl());
  }
}
