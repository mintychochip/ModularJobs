package net.aincraft.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EditorServiceTest {

  @Test
  void editorUrlKeepsTokenAfterFragment() {
    String url = EditorService.editorUrl(
        "https://editor.example/editor", "code/1", "secret-token");

    assertEquals(
        "https://editor.example/editor/session?code=code%2F1#token=secret-token",
        url);
    assertTrue(url.indexOf("#token=") > url.indexOf("code="));
  }
}
