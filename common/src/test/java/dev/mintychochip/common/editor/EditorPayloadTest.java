package dev.mintychochip.common.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EditorPayloadTest {

  @Test
  void roundTripCamelCaseJson() {
    EditorMetadata meta =
        EditorMetadata.create("2026-01-01T00:00:00Z", "player-uuid", "token-1", "server");
    EditorPayload payload =
        EditorPayload.create(
            meta,
            Map.of(
                "miner",
                new JobData(
                    "Miner",
                    List.of(
                        new TaskData(
                            "break",
                            "minecraft:stone",
                            List.of(new PayableData("exp", "10")))))),
            List.of("break"),
            List.of("exp"));

    Gson gson = new Gson();
    String json = gson.toJson(payload);
    EditorPayload back = gson.fromJson(json, EditorPayload.class);

    assertEquals(1, back.version());
    assertEquals("token-1", back.metadata().sessionToken());
    assertEquals("Miner", back.jobs().get("miner").displayName());
    assertEquals("break", back.jobs().get("miner").tasks().getFirst().actionTypeKey());
  }
}
