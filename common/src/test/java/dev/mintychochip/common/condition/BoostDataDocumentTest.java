package dev.mintychochip.common.condition;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.charset.StandardCharsets;
import java.util.List;
import dev.mintychochip.common.boost.BoostDataDocument;
import dev.mintychochip.common.boost.BoostDataDocument.BoostDocument;
import dev.mintychochip.common.boost.BoostDataDocument.RuleDocument;
import dev.mintychochip.common.boost.BoostDataDocument.SourceDocument;
import dev.mintychochip.databag.Condition;
import dev.mintychochip.databag.ConditionSerializer;
import dev.mintychochip.databag.Conditions;
import dev.mintychochip.databag.SneakingCondition;
import dev.mintychochip.databag.gson.GsonConditionSerializer;
import org.junit.jupiter.api.Test;

class BoostDataDocumentTest {

  private final ConditionSerializer serializer = GsonConditionSerializer.gson();

  @Test
  void roundTripStoresPriorityAndConditionBytes() {
    byte[] conditionBytes = serializer.write(Conditions.sneaking(true));
    BoostDataDocument document = new BoostDataDocument(
        "passive",
        "all",
        null,
        new SourceDocument(
            "modularjobs:mining_helmet",
            "helmet",
            List.of(RuleDocument.of(100, conditionBytes, new BoostDocument("multiplicative", 1.25)))
        )
    );

    byte[] json = BoostDataDocument.toJson(document);
    BoostDataDocument back = BoostDataDocument.fromJson(json);

    assertEquals("passive", back.kind());
    assertEquals("all", back.slots());
    RuleDocument rule = back.source().rules().getFirst();
    assertEquals(100, rule.priority());
    assertArrayEquals(conditionBytes, rule.conditionBytes());
    Condition decoded = serializer.read(rule.conditionBytes());
    assertInstanceOf(SneakingCondition.class, decoded);
    assertEquals("multiplicative", rule.boost().type());
  }

  @Test
  void jsonContainsBase64ConditionsNotNestedObject() {
    byte[] conditionBytes = serializer.write(Conditions.sneaking(true));
    BoostDataDocument document = new BoostDataDocument(
        "consumable",
        null,
        "PT1H",
        new SourceDocument(
            "modularjobs:timed",
            null,
            List.of(RuleDocument.of(1, conditionBytes, new BoostDocument("additive", 5)))
        )
    );
    String json = new String(BoostDataDocument.toJson(document), StandardCharsets.UTF_8);
    assertEquals(true, json.contains("\"priority\":1") || json.contains("\"priority\": 1"));
    assertEquals(true, json.contains("\"conditions\":"));
    assertEquals(false, json.contains("minecraft:entity_properties"));
  }
}
